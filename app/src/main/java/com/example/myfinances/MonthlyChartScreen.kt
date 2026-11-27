package com.example.myfinances

import android.content.Intent
import android.os.Trace
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.zIndex

@Composable
fun MonthlyChartScreen(expensesList: List<Expense>, isLoading: Boolean) {
    DisposableEffect(Unit) {
        Trace.beginSection("ComposeScreen_CategoriesLauncher")
        onDispose { Trace.endSection() }
    }

    val context = LocalContext.current
    // Stan przechowujący informację o tym, który wykres jest wybrany
    var isBarChart by remember { mutableStateOf(true) }
    // DODANA LINIA:
    val toggleInteractionSource = remember { MutableInteractionSource() }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF74C69D))
                }
            } else {
                val currentMonth = YearMonth.now()
                val last6Months = remember {
                    (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
                }

                val monthlyTotals = remember(expensesList, last6Months) {
                    last6Months.associateWith { month ->
                        expensesList.filter { YearMonth.from(it.date) == month }
                            .sumOf { it.amount.toDouble() }.toFloat()
                    }
                }

                val maxTotal = monthlyTotals.values.maxOrNull() ?: 0f
                // ZMODYFIKOWANA LOGIKA ZAOKRĄGLANIA (TA SAMA CO W XML)
                val yAxisMax = when {
                    maxTotal >= 1000f -> ((maxTotal / 500).toInt() + 1) * 500f
                    maxTotal > 0f -> ((maxTotal / 100).toInt() + 1) * 100f
                    else -> 100f
                }

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
                        modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
                    )

                    // Przełącznik (Toggle) Wykresu
                    Row(
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isBarChart) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = toggleInteractionSource,
                                    indication = null // TUTAJ CAŁKOWICIE WYŁĄCZAMY EFEKT KLIKNIĘCIA
                                ) { isBarChart = true }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Kolumnowy",
                                color = if (isBarChart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (!isBarChart) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = toggleInteractionSource,
                                    indication = null // TUTAJ CAŁKOWICIE WYŁĄCZAMY EFEKT KLIKNIĘCIA
                                ) { isBarChart = false }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Liniowy",
                                color = if (!isBarChart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

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

                            // Używamy "key", aby zmiana trybu wykresu resetowała animację od nowa
                            key(isBarChart) {
                                Box(modifier = Modifier.fillMaxSize()) {

                                    // Rysowanie wykresu liniowego pod spodem
                                    if (!isBarChart) {
                                        LineChartPath(
                                            totals = last6Months.map { monthlyTotals[it] ?: 0f },
                                            yAxisMax = yAxisMax,
                                            xAxisLabelSpace = xAxisLabelSpace
                                        )
                                    }

                                    // Etykiety, obszary klikalne i wartości (oraz słupki jeśli wybrano)
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        last6Months.forEachIndexed{ index, month ->
                                            val totalForMonth = monthlyTotals[month] ?: 0f
                                            val heightPercentage = if (yAxisMax > 0) totalForMonth / yAxisMax else 0f

                                            MonthlyChartItem(
                                                isBarChart = isBarChart,
                                                monthLabel = month.format(formatter),
                                                total = totalForMonth,
                                                heightPercentage = heightPercentage,
                                                xAxisLabelSpace = xAxisLabelSpace,
                                                index = index, // Przekazujemy indeks do synchronizacji animacji
                                                onClick = {
                                                    val intent = Intent(context, ExpandableCategoriesActivity::class.java).apply {
                                                        putExtra("SELECTED_MONTH", month.toString())
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.weight(1f) // Usunięto stąd clickable (przeniesiony do środka)
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
    }
}

@Composable
fun LineChartPath(
    totals: List<Float>,
    yAxisMax: Float,
    xAxisLabelSpace: Dp
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Animacja przejścia od 0 (lewa strona) do 1 (prawa strona)
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "LineAnimation"
    )

    LaunchedEffect(Unit) { startAnimation = true }

    val lineColor = Color(0xFF2196F3)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = xAxisLabelSpace)
    ) {
        val columnWidth = size.width / totals.size

        // Obliczanie współrzędnych punktów
        val points = totals.mapIndexed { index, total ->
            val heightPercent = if (yAxisMax > 0) total / yAxisMax else 0f
            val x = (index * columnWidth) + (columnWidth / 2)
            val y = size.height - (size.height * heightPercent)
            Offset(x, y)
        }

        // Przycięcie obszaru rysowania wymusza efekt "odkrywania" od lewej do prawej
        clipRect(right = size.width * animatedProgress) {
            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y)
                    else lineTo(point.x, point.y)
                }
            }

            // Rysowanie samej linii
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Rysowanie punktów na łączeniach (kropki)
            points.forEach { point ->
                drawCircle(color = lineColor, radius = 6.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = point)
            }
        }
    }
}

@Composable
fun MonthlyChartItem(
    isBarChart: Boolean,
    monthLabel: String,
    total: Float,
    heightPercentage: Float,
    xAxisLabelSpace: Dp,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Ujednolicony progres od 0.0 do 1.0
    val itemProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isBarChart) 1200 else 400,
            // Magia matematyki: opóźniamy start liczb wykresu liniowego tak, aby pojawiały się idealnie
            // w momencie najechania na nie rysującej się animowanej linii (ktora ma równe 1200ms dla całości).
            delayMillis = if (isBarChart) 100 else 100 + (index * 200),
            easing = FastOutSlowInEasing
        ),
        label = "MonthlyItemProgress"
    )

    LaunchedEffect(Unit) { startAnimation = true }

    val primaryColor = Color(0xFF2196F3)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
            .fillMaxHeight()
            .then(
                // Dla wykresu kolumnowego klikalna jest cała wysokość kolumny
                if (isBarChart) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {

            // 1. Rysowanie słupka
            if (isBarChart) {
                Spacer(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .drawBehind {
                            // Używamy itemProgress jako ułamka do osiągnięcia pełnego heightPercentage
                            val barHeightPx = (size.height * heightPercentage * itemProgress).coerceAtLeast(0.01f)
                            val topY = size.height - barHeightPx
                            val cornerPx = 6.dp.toPx()

                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(0f, topY),
                                size = Size(size.width, barHeightPx),
                                cornerRadius = CornerRadius(cornerPx, cornerPx)
                            )

                            if (barHeightPx > cornerPx) {
                                drawRect(
                                    color = primaryColor,
                                    topLeft = Offset(0f, topY + barHeightPx - cornerPx),
                                    size = Size(size.width, cornerPx)
                                )
                            }
                        }
                )
            }

            // 2. Obszar klikalny TYLKO dla kropki na wykresie liniowym
            if (!isBarChart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val targetHeightPx = size.height * heightPercentage
                            translationY = -targetHeightPx
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp) // Wygodny obszar dla palca (zgodny z wytycznymi Accessibility)
                            .offset(y = 24.dp) // Przesuwamy w dół o połowę, aby środek kwadratu był dokładnie na kropce
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // Brak brzydkiego kwadratowego cienia
                                onClick = onClick
                            )
                    )
                }
            }

            // 3. Tekst (Wartości liczbowe)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f) // Wymusza rysowanie tekstu zawsze na samym wierzchu
                    .graphicsLayer {
                        val finalHeightPx = size.height * heightPercentage

                        if (isBarChart) {
                            // Liczby podążają do góry z rosnącym słupkiem
                            val currentHeightPx = finalHeightPx * itemProgress
                            translationY = -currentHeightPx - 8.dp.toPx()
                            alpha = 1f
                        } else {
                            // Liczby pojawiają się statycznie, od razu w odpowiednim miejscu
                            translationY = -finalHeightPx - 16.dp.toPx()
                            alpha = itemProgress // Przezroczystość płynnie od 0 do 1
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (total > 0) {
                    Text(
                        text = "${total.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Etykieta miesiąca na dole
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