package com.example.myfinances

import android.content.Intent
import android.os.Trace
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun MonthlyChartScreen(expensesList: List<Expense>, isLoading: Boolean) {
    DisposableEffect(Unit) {
        Trace.beginSection("ComposeScreen_CategoriesLauncher")
        onDispose { Trace.endSection() }
    }

    val context = LocalContext.current

    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF74C69D))
                }
            } else {
                // Wyliczanie ostatnich 6 miesięcy
                val currentMonth = YearMonth.now()
                val last6Months = remember {
                    (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
                }

                // Sumowanie wydatków dla każdego z 6 miesięcy
                val monthlyTotals = remember(expensesList, last6Months) {
                    last6Months.associateWith { month ->
                        expensesList.filter { YearMonth.from(it.date) == month }
                            .sumOf { it.amount.toDouble() }.toFloat()
                    }
                }

                val maxTotal = monthlyTotals.values.maxOrNull() ?: 0f
                val yAxisMax = if (maxTotal > 0f) {
                    ((maxTotal / 100).toInt() + 1) * 100f
                } else 100f

                val stepCount = 4
                val stepValue = yAxisMax / stepCount
                val xAxisLabelSpace = 40.dp
                val formatter = DateTimeFormatter.ofPattern("MM/yy")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wydatki z 6 miesięcy",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp, top = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        // Etykiety osi Y
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
                            // Linie tła
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

                            // Słupki wykresu
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                last6Months.forEach { month ->
                                    val totalForMonth = monthlyTotals[month] ?: 0f
                                    val heightPercentage = if (yAxisMax > 0) totalForMonth / yAxisMax else 0f

                                    MonthlyBarChartItem(
                                        monthLabel = month.format(formatter),
                                        total = totalForMonth,
                                        heightPercentage = heightPercentage,
                                        xAxisLabelSpace = xAxisLabelSpace,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                // Przekazujemy wybrany miesiąc (np. "2026-11")
                                                val intent = Intent(context, ExpandableCategoriesActivity::class.java).apply {
                                                    putExtra("SELECTED_MONTH", month.toString())
                                                }
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
    }
}

@Composable
fun MonthlyBarChartItem(
    monthLabel: String,
    total: Float,
    heightPercentage: Float,
    xAxisLabelSpace: Dp,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val animatedFraction by animateFloatAsState(
        targetValue = if (startAnimation) heightPercentage else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            delayMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "MonthlyBarHeightAnimation"
    )

    LaunchedEffect(Unit) { startAnimation = true }

    val barColor = Color(0xFF2196F3) // Wspólny, niebieski kolor słupków

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
            Spacer(
                modifier = Modifier
                    .width(36.dp) // Zwężone pod 6 słupków na ekranie
                    .fillMaxHeight()
                    .drawBehind {
                        val barHeightPx = size.height * animatedFraction.coerceAtLeast(0.01f)
                        val topY = size.height - barHeightPx
                        val cornerPx = 6.dp.toPx()

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(0f, topY),
                            size = Size(size.width, barHeightPx),
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )

                        if (barHeightPx > cornerPx) {
                            drawRect(
                                color = barColor,
                                topLeft = Offset(0f, topY + barHeightPx - cornerPx),
                                size = Size(size.width, cornerPx)
                            )
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val barHeightPx = size.height * animatedFraction.coerceAtLeast(0.01f)
                        translationY = -barHeightPx - 4.dp.toPx()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (total > 0) {
                    Text(
                        text = "${total.toInt()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .height(xAxisLabelSpace)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}