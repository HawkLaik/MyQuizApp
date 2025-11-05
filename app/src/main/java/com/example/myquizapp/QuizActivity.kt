package com.example.myquizapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class QuizActivity : AppCompatActivity() {

    private lateinit var questionText: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var nextButton: MaterialButton
    private lateinit var errorText: TextView
    private lateinit var questionLayout: View
    private lateinit var resultLayout: View
    private lateinit var resultText: TextView
    private lateinit var backToMenuButton: MaterialButton

    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private var score = 0
    private val selectedIndices = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        initViews()
        loadQuestionsFromIntent()

        nextButton.setOnClickListener { onNextClicked() }
        backToMenuButton.setOnClickListener { finish() }
    }

    private fun initViews() {
        questionText = findViewById(R.id.questionText)
        radioGroup = findViewById(R.id.radioGroup)
        nextButton = findViewById(R.id.nextButton)
        errorText = findViewById(R.id.errorText)
        questionLayout = findViewById(R.id.questionLayout)
        resultLayout = findViewById(R.id.resultLayout)
        resultText = findViewById(R.id.resultText)
        backToMenuButton = findViewById(R.id.backToMenuButton)
    }

    private fun loadQuestionsFromIntent() {
        val testId = intent.getIntExtra("TEST_ID", 1)
        questions = loadQuestionsForTest(testId)

        if (questions.isEmpty()) {
            Toast.makeText(this, "Ошибка загрузки вопросов для теста $testId", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        showQuestion()
    }

    private fun loadQuestionsForTest(testId: Int): List<Question> {
        return try {
            val jsonString = assets.open("questions.json").bufferedReader().use { it.readText() }
            val allQuestions = parseQuestions(jsonString)
            val startIndex = (testId - 1) * 10
            val endIndex = minOf(startIndex + 10, allQuestions.size)

            if (startIndex < allQuestions.size) {
                allQuestions.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки вопросов: ${e.message}", Toast.LENGTH_LONG).show()
            emptyList()
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

    private fun showQuestion() {
        val q = questions[currentQuestionIndex]
        questionText.text = "Вопрос ${currentQuestionIndex + 1} из ${questions.size}\n\n${q.question}"

        radioGroup.removeAllViews()
        selectedIndices.clear()
        errorText.visibility = View.GONE
        nextButton.isEnabled = true

        val letters = listOf("А", "Б", "В", "Г", "Д", "Е")

        q.options.forEachIndexed { index, option ->
            val card = MaterialCardView(this).apply {
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                setCardBackgroundColor(ContextCompat.getColor(this@QuizActivity, R.color.obsidian_surface))
                setStrokeColor(ContextCompat.getColorStateList(this@QuizActivity, R.color.obsidian_border)!!)
                radius = 8f
                setContentPadding(16, 16, 16, 16)
            }

            val checkBox = CheckBox(this).apply {
                id = View.generateViewId()
                text = "${letters[index]}. $option"
                setTextColor(ContextCompat.getColorStateList(this@QuizActivity, R.color.obsidian_text_primary))
                buttonTintList = ContextCompat.getColorStateList(this@QuizActivity, R.color.obsidian_accent)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIndices.add(index) else selectedIndices.remove(index)
                updateCardStyle(card, index, q.correctIndices)
            }

            card.addView(checkBox)
            radioGroup.addView(card)
        }

        resetCardStyles()
    }

    private fun updateCardStyle(card: MaterialCardView, index: Int, correctIndices: List<Int>) {
        val isSelected = index in selectedIndices
        val isCorrect = index in correctIndices
        val colorRes = when {
            isSelected && isCorrect -> R.color.green_correct
            isSelected && !isCorrect -> R.color.red_wrong
            else -> R.color.obsidian_border
        }
        card.setStrokeColor(ContextCompat.getColorStateList(this, colorRes)!!)
    }

    private fun resetCardStyles() {
        for (i in 0 until radioGroup.childCount) {
            val card = radioGroup.getChildAt(i) as MaterialCardView
            card.setStrokeColor(ContextCompat.getColorStateList(this, R.color.obsidian_border)!!)
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.obsidian_surface))
        }
    }

    private fun onNextClicked() {
        if (selectedIndices.isEmpty()) {
            errorText.visibility = View.VISIBLE
            return
        }

        errorText.visibility = View.GONE
        val q = questions[currentQuestionIndex]
        val correctSet = q.correctIndices.toSet()
        val selectedSet = selectedIndices.toSet()
        val isPerfect = selectedSet == correctSet
        if (isPerfect) score++

        q.options.forEachIndexed { index, _ ->
            val card = radioGroup.getChildAt(index) as MaterialCardView
            val isSelected = index in selectedIndices
            val isCorrect = index in q.correctIndices

            val colorRes = when {
                isSelected && isCorrect -> R.color.green_correct
                isSelected && !isCorrect -> R.color.red_wrong
                !isSelected && isCorrect -> R.color.green_correct
                else -> R.color.obsidian_surface
            }

            card.setStrokeColor(ContextCompat.getColorStateList(this, colorRes)!!)
            if (isSelected || isCorrect) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, colorRes))
            }
        }

        nextButton.isEnabled = false
        nextButton.postDelayed({
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                showQuestion()
                nextButton.isEnabled = true
            } else {
                showResult()
            }
        }, 1500)
    }

    private fun showResult() {
        questionLayout.visibility = View.GONE
        resultLayout.visibility = View.VISIBLE
        resultText.text = "Результат: $score из ${questions.size}"
    }
}

// Data class для вопроса (добавь в конец файла)
data class Question(
    val question: String,
    val options: List<String>,
    val correctIndices: List<Int>
)