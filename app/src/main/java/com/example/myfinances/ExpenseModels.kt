package com.example.myfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// 1. Zbiór dostępnych kategorii
enum class ExpenseCategory(val displayName: String, val color: Color) {
    FOOD("Jedzenie", Color(0xFF4CAF50)),
    TRANSPORT("Transport", Color(0xFF2196F3)),
    ENTERTAINMENT("Rozrywka", Color(0xFFFF9800)),
    BILLS("Rachunki", Color(0xFFF44336))
}

// 2. Model pojedynczego wydatku
data class Expense(
    val name: String,
    val amount: Float,
    val date: LocalDate,
    val category: ExpenseCategory
)

// 3. Przykładowe dane
val mockExpenses = listOf(
    Expense("Biedronka - zakupy", 150.50f, LocalDate.of(2026, 9, 20), ExpenseCategory.FOOD),
    Expense("Paliwo Orlen", 250.00f, LocalDate.of(2026, 9, 21), ExpenseCategory.TRANSPORT),
    Expense("Kino", 85.00f, LocalDate.of(2026, 9, 22), ExpenseCategory.ENTERTAINMENT),
    Expense("Lidl", 90.00f, LocalDate.of(2026, 9, 23), ExpenseCategory.FOOD),
    Expense("Prąd", 140.00f, LocalDate.of(2026, 9, 24), ExpenseCategory.BILLS),
    Expense("Bilet miesięczny", 110.00f, LocalDate.of(2026, 9, 25), ExpenseCategory.TRANSPORT),
    Expense("Wyjście na miasto", 120.00f, LocalDate.of(2026, 9, 25), ExpenseCategory.ENTERTAINMENT)
)