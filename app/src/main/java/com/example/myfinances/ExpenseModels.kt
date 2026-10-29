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
    BILLS("Rachunki", Color(0xFFF44336)),
    OTHER("Inne", Color(0xFF9E9E9E))
}

// 2. Model pojedynczego wydatku
// Zaktualizuj swoją klasę Expense:
data class Expense(
    val id: String,
    val name: String,
    val amount: Float,
    val date: LocalDate,
    val category: ExpenseCategory
)

val mockExpenses = listOf(
    Expense("", "Biedronka - zakupy", 150.5f, LocalDate.now(), ExpenseCategory.FOOD),
    Expense("", "Paliwo Orlen", 200.0f, LocalDate.now().minusDays(1), ExpenseCategory.TRANSPORT),
    Expense("", "Kino", 45.0f, LocalDate.now().minusDays(2), ExpenseCategory.ENTERTAINMENT),
    Expense("", "Abonament Spotify", 20.0f, LocalDate.now().minusDays(3), ExpenseCategory.OTHER)
)