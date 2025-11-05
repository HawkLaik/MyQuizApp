package com.example.myquizapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tests: List<Test>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        recyclerView = findViewById(R.id.testsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadTests()
    }

    private fun loadTests() {
        try {
            val jsonString = assets.open("questions.json").bufferedReader().use { it.readText() }
            val allQuestions = parseQuestions(jsonString)

            // Разбиваем на 10 тестов по 10 вопросов
            tests = allQuestions.chunked(10).mapIndexed { index, questions ->
                Test(
                    id = index + 1,
                    title = "Тест ${index + 1}",
                    questions = questions
                )
            }

            val adapter = TestAdapter(tests) { test ->
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("TEST_ID", test.id) // ← передаем только ID
                }
                startActivity(intent)
            }
            recyclerView.adapter = adapter

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки тестов: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun parseQuestions(jsonString: String): List<Question> {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(jsonString.trim())

        val jsonArray = when (root) {
            is kotlinx.serialization.json.JsonArray -> root
            is kotlinx.serialization.json.JsonObject -> {
                root.jsonObject.entries.firstOrNull { it.value is kotlinx.serialization.json.JsonArray }
                    ?.value as? kotlinx.serialization.json.JsonArray
                    ?: throw IllegalArgumentException("Массив вопросов не найден")
            }
            else -> throw IllegalArgumentException("Неверный формат JSON")
        }

        return jsonArray.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                val questionText = obj["question"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val options = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: return@mapNotNull null

                val correctJson = obj["correctIndex"] ?: return@mapNotNull null
                val correctIndices = when {
                    correctJson is kotlinx.serialization.json.JsonPrimitive ->
                        listOf(correctJson.content.toInt())
                    correctJson is kotlinx.serialization.json.JsonArray ->
                        correctJson.mapNotNull { it.jsonPrimitive.content.toIntOrNull() }
                    else -> emptyList()
                }

                Question(questionText, options, correctIndices)
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Добавь эти data class в конец файла MainActivity.kt
data class Test(
    val id: Int,
    val title: String,
    val questions: List<Question>
)