package com.example.myquizapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class TestAdapter(private val tests: List<Test>, private val onTestClick: (Test) -> Unit) :
    RecyclerView.Adapter<TestAdapter.TestViewHolder>() {

    class TestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.testCard)
        val title: TextView = view.findViewById(R.id.testTitle)
        val questionsCount: TextView = view.findViewById(R.id.questionsCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test, parent, false)
        return TestViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]
        holder.title.text = "Тест ${position + 1}"
        holder.questionsCount.text = "${test.questions.size} вопросов"

        holder.card.setOnClickListener {
            onTestClick(test)
        }
    }

    override fun getItemCount() = tests.size
}