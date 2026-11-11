package com.example.myfinances

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat

class CategoriesChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dane zasilające: Kategoria -> Kwota
    private var chartData = listOf<Pair<ExpenseCategory, Float>>()
    // DODANO: Zapamiętywanie poprzedniego stanu do animowania tylko dodanego wydatku
    private var previousData = mapOf<ExpenseCategory, Float>()

    private var yAxisMax = 100f
    private var animationProgress = 0f

    private var onCategoryClickListener: ((ExpenseCategory) -> Unit)? = null
    private var animator: ValueAnimator? = null

    // Pędzle graficzne (Paints) - USUNIĘTO textPaint dla nazw kategorii
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val yAxisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 32f
        textAlign = Paint.Align.RIGHT
    }
    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }

    // Wymiary przestrzenne (dopasowane do proporcji 7 kategorii)
    // ZMNIEJSZONO xAxisSpace, ponieważ nie ma już pod spodem tekstu
    private val xAxisSpace = 80f
    private val yAxisSpace = 130f
    private val topSpace = 80f
    private val barWidth = 95f    // Słupki są cieńsze, by zmieścić 7 sztuk
    private val iconSize = 60     // Rozmiar ikon na dole

    // Mapa z kolorowanymi ikonami dla optymalizacji
    private val categoryIconsCache = mutableMapOf<ExpenseCategory, Drawable>()

    fun setData(data: List<Pair<ExpenseCategory, Float>>) {
        // Zapisujemy stary stan, żeby wiedzieć, który słupek ma urosnąć
        previousData = this.chartData.toMap()
        this.chartData = data
        val maxTotal = chartData.maxOfOrNull { it.second } ?: 0f

        // Dynamiczne wyliczanie siatki (kroki Y) - zapobieganie błędom matematycznym
        yAxisMax = when {
            maxTotal >= 1000f -> ((maxTotal / 500).toInt() + 1) * 500f
            maxTotal > 0f -> ((maxTotal / 100).toInt() + 1) * 100f
            else -> 100f
        }

        loadIcons()
        startAnimation()
    }

    fun setOnCategoryClickListener(listener: (ExpenseCategory) -> Unit) {
        this.onCategoryClickListener = listener
    }

    private fun loadIcons() {
        categoryIconsCache.clear()
        chartData.forEach { (category, _) ->
            val iconResId = when (category.name) {
                "FOOD" -> R.drawable.baseline_directions_car_24
                "TRANSPORT" -> R.drawable.baseline_directions_car_24
                "ENTERTAINMENT" -> R.drawable.baseline_directions_car_24
                "BILLS" -> R.drawable.baseline_directions_car_24
                "CLOTHING" -> R.drawable.baseline_directions_car_24
                "HEALTH" -> R.drawable.baseline_directions_car_24
                else -> R.drawable.baseline_directions_car_24
            }
            ContextCompat.getDrawable(context, iconResId)?.let { drawable ->
                val mutatedDrawable = drawable.mutate()
                mutatedDrawable.setTint(category.color.toArgb())
                categoryIconsCache[category] = mutatedDrawable
            }
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animationProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            startDelay = 100
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chartData.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val chartWidth = viewWidth - yAxisSpace

        val chartBottom = viewHeight - xAxisSpace
        val drawingHeight = chartBottom - topSpace

        // 1. Rysowanie poziomych linii siatki i etykiet osi Y
        val stepCount = 4
        val stepValue = yAxisMax / stepCount
        for (i in 0..stepCount) {
            val y = chartBottom - (drawingHeight * (i.toFloat() / stepCount))
            canvas.drawText("${(stepValue * i).toInt()}", yAxisSpace - 20f, y + 10f, yAxisTextPaint)
            canvas.drawLine(yAxisSpace, y, viewWidth, y, gridPaint)
        }

        val columnWidth = chartWidth / chartData.size
        val density = resources.displayMetrics.density
        val cornerRadius = 6f * density

        // 2. Rysowanie słupków, kwot i ikon
        chartData.forEachIndexed { index, pair ->
            val (category, total) = pair
            val oldTotal = previousData[category] ?: 0f

            // DODANO: Wyliczamy aktualną wysokość rosnącego słupka od starej do nowej wartości
            val currentTotal = oldTotal + (total - oldTotal) * animationProgress

            val xCenter = yAxisSpace + (index * columnWidth) + (columnWidth / 2)
            val heightPercent = if (yAxisMax > 0) currentTotal / yAxisMax else 0f

            // Używamy currentTotal (wzrastającego) zamiast mnożyć przez animationProgress drugi raz
            val currentHeight = drawingHeight * heightPercent.coerceAtLeast(0.01f)
            val topY = chartBottom - currentHeight

            barPaint.color = category.color.toArgb()

            // Rysowanie zaokrąglonego prostokąta (słupek)
            val rect = RectF(xCenter - (barWidth / 2), topY, xCenter + (barWidth / 2), chartBottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)

            if (currentHeight > cornerRadius) {
                canvas.drawRect(
                    xCenter - (barWidth / 2),
                    chartBottom - cornerRadius,
                    xCenter + (barWidth / 2),
                    chartBottom,
                    barPaint
                )
            }

            // Animowane kwoty - wyświetla od razu wartość docelową (total), żeby liczby się nie zmieniały
            if (total > 0) {
                valueTextPaint.alpha = 255 // <--- Zawsze 100% widoczności
                canvas.drawText("${total.toInt()} zł", xCenter, topY - 15f, valueTextPaint)
            }

            // Rysowanie Ikony wycentrowanej pod linią 0
            val iconTop = chartBottom + 16f // Zmniejszono margines pod osią, żeby ikony były bliżej
            categoryIconsCache[category]?.let { icon ->
                val halfIcon = iconSize / 2
                icon.setBounds(
                    (xCenter - halfIcon).toInt(),
                    iconTop.toInt(),
                    (xCenter + halfIcon).toInt(),
                    (iconTop + iconSize).toInt()
                )
                icon.draw(canvas)
            }

            // USUNIĘTO blok rysujący pochyły tekst z nazwą kategorii
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val chartWidth = width - yAxisSpace
            val columnWidth = chartWidth / chartData.size

            if (event.x > yAxisSpace) {
                val clickedIndex = ((event.x - yAxisSpace) / columnWidth).toInt()

                if (clickedIndex in chartData.indices) {
                    onCategoryClickListener?.invoke(chartData[clickedIndex].first)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}