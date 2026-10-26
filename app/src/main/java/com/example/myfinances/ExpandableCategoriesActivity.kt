package com.example.myfinances

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfinances.ui.theme.MyFinancesTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

class ExpandableCategoriesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFinancesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpandableCategoriesScreen()
                }
            }
        }
    }
}

@Composable
fun ExpandableCategoriesScreen() {
    var groupedExpenses by remember { mutableStateOf<Map<ExpenseCategory, List<Expense>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedExpenses = mutableListOf<Expense>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                    val dateStr = child.child("date").getValue(String::class.java) ?: LocalDate.now().toString()

                    // Bezpieczne pobieranie kategorii z fallbackiem do OTHER
                    val catStr = child.child("category").getValue(String::class.java) ?: ExpenseCategory.OTHER.name

                    fetchedExpenses.add(Expense(name, amount, LocalDate.parse(dateStr), ExpenseCategory.valueOf(catStr)))
                }

                groupedExpenses = fetchedExpenses.groupBy { it.category }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        }
        databaseRef.addValueEventListener(listener)
        onDispose { databaseRef.removeEventListener(listener) }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Optymalizacja: Dodany parametr key zapobiegający zbędnym rekompozycjom całej listy
            items(
                items = groupedExpenses.keys.toList(),
                key = { category -> category.name }
            ) { category ->
                val expenses = groupedExpenses[category] ?: emptyList()
                ExpandableCategoryCard(category, expenses)
            }
        }
    }
}

@Composable
fun ExpandableCategoryCard(category: ExpenseCategory, expenses: List<Expense>) {
    var expanded by remember { mutableStateOf(false) }

    // Pobranie kontekstu wymaganego do uruchomienia nowej aktywności
    val context = LocalContext.current

    // Optymalizacja: Zapamiętanie sumy, aby nie przeliczać jej na nowo w każdej klatce animacji
    val totalSum = remember(expenses) {
        expenses.sumOf { it.amount.toDouble() }.toFloat()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // Automatyczna animacja zmiany rozmiaru
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Nagłówek kategorii
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(category.color.toArgb()), shape = MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = category.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${totalSum.toInt()} zł",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Rozwiń/Zwiń"
                    )
                }
            }

            // Rozwijana lista wydatków
            if (expanded) {
                Divider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenses.forEach { expense ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // NOWE: Dodanie klikalności z efektem wizualnym dla pojedynczego wydatku
                                .clickable {
                                    val intent = Intent(context, MainActivity::class.java)
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp), // Lekki padding dla wygodniejszego klikania palcem
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = expense.name, color = Color.DarkGray)
                            Text(text = "- ${expense.amount.toInt()} zł", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}