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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.DirectionsCar // Auto
import androidx.compose.material.icons.filled.List          // Inne
import androidx.compose.material.icons.filled.MusicNote     // Nutka
import androidx.compose.material.icons.filled.ReceiptLong   // Rachunek/Lista
import androidx.compose.material.icons.filled.ShoppingCart  // Jedzenie

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
                    val id = child.key ?: "" // Pobranie unikalnego klucza z Firebase
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                    val dateStr = child.child("date").getValue(String::class.java) ?: LocalDate.now().toString()
                    val catStr = child.child("category").getValue(String::class.java) ?: ExpenseCategory.OTHER.name

                    fetchedExpenses.add(Expense(id, name, amount, LocalDate.parse(dateStr), ExpenseCategory.valueOf(catStr)))
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
        // DODANO: Wymuszenie stałej kolejności kategorii
        val sortedCategories = remember(groupedExpenses) {
            val order = listOf("FOOD", "TRANSPORT", "ENTERTAINMENT", "BILLS", "OTHER")
            groupedExpenses.keys.sortedBy { category ->
                val index = order.indexOf(category.name)
                // Jeśli kategoria nie jest na liście order, ląduje na samym końcu
                if (index == -1) Int.MAX_VALUE else index
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Optymalizacja: Dodany parametr key zapobiegający zbędnym rekompozycjom całej listy
            items(
                items = sortedCategories,
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
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val totalSum = remember(expenses) {
        expenses.sumOf { it.amount.toDouble() }.toFloat()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // MAPOWANIE: Wybór ikony na podstawie nazwy kategorii
                    val categoryIcon = when(category.name) {
                        "FOOD" -> Icons.Default.ShoppingCart
                        "TRANSPORT" -> Icons.Default.DirectionsCar // Zmienione na Place, bo masz już ten import na górze pliku i nie wyrzuci błędu
                        "ENTERTAINMENT" -> Icons.Default.MusicNote
                        "BILLS" -> Icons.Default.ReceiptLong
                        else -> Icons.Default.List // Domyślna ikona
                    }

                    // ZMIANA: Icon zamiast Box z tłem
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category.displayName,
                        modifier = Modifier.size(28.dp),
                        tint = Color(category.color.toArgb()) // Ikona przyjmuje kolor kategorii
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = category.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // DODANO FORMATOWANIE:
                    Text(
                        text = String.format("%.2f zł", totalSum),
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
                                .clickable {
                                    // DODANO PRZEJŚCIE DO SZCZEGÓŁÓW Z DANYMI (tutaj ładujemy ExpenseDetailXMLActivity)
                                    val intent = Intent(context, ExpenseDetailActivity::class.java).apply {
                                        putExtra("EXTRA_ID", expense.id)
                                        putExtra("EXTRA_NAME", expense.name)
                                        putExtra("EXTRA_AMOUNT", expense.amount)
                                        putExtra("EXTRA_DATE", expense.date.toString())
                                        putExtra("EXTRA_CATEGORY", expense.category.name)
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = expense.name, color = Color.DarkGray)
                            // DODANO FORMATOWANIE:
                            Text(text = String.format("- %.2f zł", expense.amount), color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}