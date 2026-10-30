package com.example.myfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myfinances.ui.theme.MyFinancesTheme

class ExpenseDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Odbieranie danych z Intentu
        val name = intent.getStringExtra("EXTRA_NAME") ?: "Brak nazwy"
        val amount = intent.getFloatExtra("EXTRA_AMOUNT", 0f)
        val date = intent.getStringExtra("EXTRA_DATE") ?: "Brak daty"
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "INNE"

        setContent {
            MyFinancesTheme {
                // Ustawienie jasnoszarego tła analogicznie do #F5F5F5 z pliku XML
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    ExpenseDetailScreen(
                        name = name,
                        amount = amount,
                        date = date,
                        category = category
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseDetailScreen(name: String, amount: Float, date: String, category: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            // Formatowanie do 2 miejsc po przecinku
            text = String.format("%.2f zł", amount),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F), // Ciemnoczerwony kolor kwoty użyty wcześniej w XML
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Data: $date",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Kategoria: $category",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )
    }
}