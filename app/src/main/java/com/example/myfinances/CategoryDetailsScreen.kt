package com.example.myfinances

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CategoryDetailsScreen(
    categoryName: String,
    expensesList: List<Expense>,
    isLoading: Boolean,
    onExpenseClick: (String) -> Unit
) {
    val filteredExpenses = remember(expensesList, categoryName) {
        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year
        expensesList
            .filter {
                it.category.name == categoryName &&
                        it.date.monthValue == currentMonth &&
                        it.date.year == currentYear
            }
            .sortedByDescending { it.amount }
    }

    val dynamicColors = remember(filteredExpenses) {
        filteredExpenses.indices.map { index ->
            val hue = (index * 137.508f) % 360f
            Color.hsv(hue = hue, saturation = 0.7f, value = 0.9f)
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val totalSum = filteredExpenses.sumOf { it.amount.toDouble() }.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Szczegóły: $categoryName",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = String.format("Razem: %.2f zł", totalSum),
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .size(250.dp)
            ) {
                var rotationAngle by remember { mutableFloatStateOf(0f) }

                val textPaint = remember {
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 42f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var lastAngle = 0f
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    lastAngle = Math.toDegrees(
                                        atan2(
                                            (offset.y - cy).toDouble(),
                                            (offset.x - cx).toDouble()
                                        )
                                    ).toFloat()
                                },
                                onDrag = { change, _ ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val currentAngle = Math.toDegrees(
                                        atan2(
                                            (change.position.y - cy).toDouble(),
                                            (change.position.x - cx).toDouble()
                                        )
                                    ).toFloat()

                                    rotationAngle += (currentAngle - lastAngle)
                                    lastAngle = currentAngle
                                }
                            )
                        }
                ) {
                    var startAngle = -90f + rotationAngle
                    val strokeWidthPx = 150f

                    val padding = strokeWidthPx / 2f
                    val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                    val radius = (size.width - strokeWidthPx) / 2f

                    filteredExpenses.forEachIndexed { index, expense ->
                        val sweepAngle =
                            if (totalSum > 0) (expense.amount / totalSum) * 360f else 0f

                        drawArc(
                            color = dynamicColors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                            size = arcSize,
                            topLeft = Offset(padding, padding)
                        )

                        val percentage =
                            if (totalSum > 0) ((expense.amount / totalSum) * 100).toInt() else 0

                        if (percentage > 4) {
                            val middleAngle =
                                Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            val textX = cx + radius * cos(middleAngle).toFloat()
                            val textY =
                                cy + radius * sin(middleAngle).toFloat() - (textPaint.descent() + textPaint.ascent()) / 2

                            drawContext.canvas.nativeCanvas.drawText(
                                "$percentage%",
                                textX,
                                textY,
                                textPaint
                            )
                        }

                        startAngle += sweepAngle
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredExpenses.withIndex().toList()) { (index, expense) ->
                    val percentage =
                        if (totalSum > 0) ((expense.amount / totalSum) * 100).toInt() else 0
                    val itemColor = dynamicColors[index]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpenseClick(expense.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(itemColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = expense.name, fontSize = 16.sp, color = Color.DarkGray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("- %.2f zł", expense.amount),
                                color = itemColor
                            )
                            Text(text = "$percentage%", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}