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

class ExpensesChartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpensesScreen(expenses = mockExpenses)
                }
            }
        }
    }
}

@Composable
fun ExpensesScreen(expenses: List<Expense>) {
    // Pobieramy kontekst potrzebny do uruchomienia nowej Aktywności
    val context = LocalContext.current

    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
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
                                    // ZMIANA: Obsługa kliknięcia i uruchomienie MainActivity
                                    val intent = Intent(context, MainActivity::class.java)
                                    // Opcjonalnie: możemy przekazać wybraną kategorię do MainActivity
                                    // intent.putExtra("SELECTED_CATEGORY", category.name)
                                    context.startActivity(intent)
                                }
                        )
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
    // 1. Zmienna stanu - czy animacja ma ruszyć
    var startAnimation by remember { mutableStateOf(false) }

    // 2. Deklaracja płynnej animacji wartości Float (od 0 do docelowego % wysokości)
    val animatedHeight by animateFloatAsState(
        targetValue = if (startAnimation) heightPercentage else 0f,
        animationSpec = tween(
            durationMillis = 1200, // Czas trwania: 1.2 sekundy
            delayMillis = 100,     // Krótkie opóźnienie po starcie
            easing = FastOutSlowInEasing // Zwalnia pod koniec (efekt naturalny)
        ),
        label = "BarGrowthAnimation"
    )

    // 3. Uruchomienie animacji od razu po wyrysowaniu elementu (wejściu do kompozycji)
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                if (total > 0) {
                    Text(
                        text = "${total.toInt()} zł",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(44.dp)
                        // TUTAJ ZMIANA: Zamiast sztywnego heightPercentage używamy animatedHeight
                        .fillMaxHeight(animatedHeight.coerceAtLeast(0.01f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(category.color)
                )
            }
        }

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