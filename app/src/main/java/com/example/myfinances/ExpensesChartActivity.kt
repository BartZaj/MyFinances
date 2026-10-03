package com.example.myfinances

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myfinances.ui.theme.MyFinancesTheme
import android.util.Log
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

class ExpensesChartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpensesScreen()
                }
            }
        }
    }
}

@Composable
fun ExpensesScreen() {
    val context = LocalContext.current

    // Zmienne stanu: lista wydatków oraz flaga ładowania
    var expensesList by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Asynchroniczne pobieranie danych z Firebase przy starcie ekranu
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isLoading = false
            return@LaunchedEffect
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // Baza pusta: zapisujemy mockExpenses do Firebase
                    mockExpenses.forEach { expense ->
                        val newExpenseRef = databaseRef.push()
                        val expenseData = mapOf(
                            "name" to expense.name,
                            "amount" to expense.amount,
                            "date" to expense.date.toString(),
                            "category" to expense.category.name
                        )
                        newExpenseRef.setValue(expenseData)
                    }
                    expensesList = mockExpenses
                    isLoading = false
                } else {
                    // Baza ma dane: pobieramy i mapujemy na obiekty
                    val fetchedExpenses = mutableListOf<Expense>()
                    for (child in snapshot.children) {
                        try {
                            val name = child.child("name").getValue(String::class.java) ?: ""
                            val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val dateStr = child.child("date").getValue(String::class.java) ?: ""
                            val categoryStr = child.child("category").getValue(String::class.java) ?: ""

                            val date = LocalDate.parse(dateStr)
                            val category = ExpenseCategory.valueOf(categoryStr)

                            fetchedExpenses.add(Expense(name, amount, date, category))
                        } catch (e: Exception) {
                            Log.e("Firebase", "Błąd parsowania wydatku: ${e.message}")
                        }
                    }
                    expensesList = fetchedExpenses
                    isLoading = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd bazy danych: ${error.message}")
                isLoading = false
            }
        })
    }

    if (isLoading) {
        // Ekran ładowania
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF74C69D))
        }
    } else {
        // Obliczenia na podstawie pobranej listy z Firebase
        val categoryTotals = remember(expensesList) {
            expensesList.groupBy { it.category }
                .mapValues { entry ->
                    entry.value.sumOf { it.amount.toDouble() }.toFloat()
                }
        }

        val maxTotal = categoryTotals.values.maxOrNull() ?: 0f

        val yAxisMax = if (maxTotal > 0f) {
            ((maxTotal / 100).toInt() + 1) * 100f
        } else 100f

        val stepCount = 4
        val stepValue = yAxisMax / stepCount

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Podsumowanie Wydatków Compose",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp, top = 16.dp)
            )

            val xAxisLabelSpace = 40.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = xAxisLabelSpace, end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in stepCount downTo 0) {
                        Text(
                            text = "${(stepValue * i).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.offset(y = (-8).dp)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = xAxisLabelSpace),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in stepCount downTo 0) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                            ) {
                                drawLine(
                                    color = Color.LightGray,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        ExpenseCategory.values().forEach { category ->
                            val totalForCategory = categoryTotals[category] ?: 0f
                            val heightPercentage = if (yAxisMax > 0) totalForCategory / yAxisMax else 0f

                            BarChartItem(
                                category = category,
                                total = totalForCategory,
                                heightPercentage = heightPercentage,
                                xAxisLabelSpace = xAxisLabelSpace,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val intent = Intent(context, MainActivity::class.java)
                                        context.startActivity(intent)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartItem(
    category: ExpenseCategory,
    total: Float,
    heightPercentage: Float,
    xAxisLabelSpace: Dp,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Animujemy bezpośrednio do docelowego procentu wysokości.
    // Dzięki temu wykres płynnie zareaguje na zmianę danych w bazie (np. edycję wydatku)
    val animatedFraction by animateFloatAsState(
        targetValue = if (startAnimation) heightPercentage else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            delayMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "BarHeightAnimation"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxHeight()
    ) {
        // Główny kontener wykresu (stały rozmiar)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 1. Słupek rysowany w fazie Draw
            Spacer(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val barHeightPx = size.height * animatedFraction.coerceAtLeast(0.01f)
                        val topY = size.height - barHeightPx
                        val cornerPx = 6.dp.toPx()

                        drawRoundRect(
                            color = category.color,
                            topLeft = Offset(0f, topY),
                            size = Size(size.width, barHeightPx),
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )

                        if (barHeightPx > cornerPx) {
                            drawRect(
                                color = category.color,
                                topLeft = Offset(0f, topY + barHeightPx - cornerPx),
                                size = Size(size.width, cornerPx)
                            )
                        }
                    }
            )

            // 2. Tekst nad słupkiem
            // Wrapper przejmuje 100% wysokości, by poprawnie wyliczyć przesunięcie
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Tutaj size.height oznacza pełną wysokość obszaru wykresu
                        val barHeightPx = size.height * animatedFraction.coerceAtLeast(0.01f)

                        // Przesuwamy tekst w górę o wysokość słupka + 4.dp marginesu nad nim
                        translationY = -barHeightPx - 4.dp.toPx()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (total > 0) {
                    Text(
                        text = "${total.toInt()} zł",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // 3. Etykieta pod osią X
        Box(
            modifier = Modifier
                .height(xAxisLabelSpace)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}