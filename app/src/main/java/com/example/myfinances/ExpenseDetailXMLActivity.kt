package com.example.myfinances

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ExpenseDetailXMLActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        val tvName: TextView = findViewById(R.id.tvDetailName)
        val tvAmount: TextView = findViewById(R.id.tvDetailAmount)
        val tvDate: TextView = findViewById(R.id.tvDetailDate)
        val tvCategory: TextView = findViewById(R.id.tvDetailCategory)

        // Odbieranie danych
        val name = intent.getStringExtra("EXTRA_NAME") ?: "Brak nazwy"
        val amount = intent.getFloatExtra("EXTRA_AMOUNT", 0f)
        val date = intent.getStringExtra("EXTRA_DATE") ?: "Brak daty"
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "INNE"

        // Ustawianie danych w widokach
        tvName.text = name
        tvAmount.text = String.format("%.2f zł", amount)
        tvDate.text = "Data: $date"
        tvCategory.text = "Kategoria: $category"
    }
}