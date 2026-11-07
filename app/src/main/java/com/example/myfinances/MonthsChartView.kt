package com.example.myfinances

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.time.YearMonth

class MonthsChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var chartData = listOf<Pair<String, Float>>()
    private var rawMonths = listOf<YearMonth>()

    private var isBarChart = true
    private var yAxisMax = 100f
    private var animationProgress = 0f

    private var onMonthClickListener: ((YearMonth) -> Unit)? = null
    private var animator: ValueAnimator? = null

    // Pędzle
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Ujednolicony niebieski kolor dla wykresu z ostatnich 6 miesięcy
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
    }
    private val dotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }
    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
        // Drobniejszy tekst odpowiadający textSize="12sp" z layoutu XML
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }

    private val xAxisSpace = 100f
    // Zwiększono margines lewy ze 80f na 130f, aby zapobiec ucinaniu większych kwot
    private val yAxisSpace = 130f
    private val topSpace = 80f
    private val barWidth = 90f // Szerokość dopasowana do layoutu 44dp

    fun setData(data: List<Pair<String, Float>>, raw: List<YearMonth>) {
        this.chartData = data
        this.rawMonths = raw
        val maxTotal = chartData.maxOfOrNull { it.second } ?: 0f
        // Zmodyfikowana logika wyliczania zaokrągleń
        yAxisMax = when {
            maxTotal >= 1000f -> ((maxTotal / 500).toInt() + 1) * 500f
            maxTotal > 0f -> ((maxTotal / 100).toInt() + 1) * 100f
            else -> 100f
        }
        startAnimation()
    }

    fun setChartType(isBar: Boolean) {
        if (this.isBarChart != isBar) {
            this.isBarChart = isBar
            startAnimation()
        }
    }

    fun setOnMonthClickListener(listener: (YearMonth) -> Unit) {
        this.onMonthClickListener = listener
    }

    private fun startAnimation() {
        animator?.cancel()
        animationProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            startDelay = 100 // Opóźnienie startu analogiczne do wykresu z kategoriami
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

        val width = width.toFloat()
        val height = height.toFloat()
        val chartWidth = width - yAxisSpace

        val chartBottom = height - xAxisSpace
        val drawingHeight = chartBottom - topSpace

        val stepCount = 4
        val stepValue = yAxisMax / stepCount
        for (i in 0..stepCount) {
            val y = chartBottom - (drawingHeight * (i.toFloat() / stepCount))
            canvas.drawText("${(stepValue * i).toInt()}", yAxisSpace - 20f, y + 10f, textPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawLine(yAxisSpace, y, width, y, gridPaint)
        }

        val columnWidth = chartWidth / chartData.size

        if (isBarChart) {
            drawBarChart(canvas, columnWidth, chartBottom, drawingHeight, height)
        } else {
            drawLineChart(canvas, columnWidth, chartBottom, drawingHeight, chartWidth, height)
        }
    }

    private fun drawBarChart(canvas: Canvas, columnWidth: Float, chartBottom: Float, drawingHeight: Float, viewHeight: Float) {
        textPaint.textAlign = Paint.Align.CENTER

        // Gęstość pikseli potrzebna do wyliczenia rogu (odpowiada 6dp)
        val density = resources.displayMetrics.density
        val cornerRadius = 6f * density

        chartData.forEachIndexed { index, pair ->
            val (label, total) = pair
            val xCenter = yAxisSpace + (index * columnWidth) + (columnWidth / 2)
            val heightPercent = if (yAxisMax > 0) total / yAxisMax else 0f

            // Wysokość min to 0.01f jak w spacer_top
            val targetHeight = drawingHeight * heightPercent.coerceAtLeast(0.01f)
            val currentHeight = targetHeight * animationProgress

            val topY = chartBottom - currentHeight

            // Rysowanie zaokrąglonego prostokąta
            val rect = RectF(xCenter - (barWidth / 2), topY, xCenter + (barWidth / 2), chartBottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)

            // Usunięcie zaokrąglenia z DOŁU słupka (nadrysowujemy zwykły prostokąt nad dolną częścią)
            if (currentHeight > cornerRadius) {
                canvas.drawRect(
                    xCenter - (barWidth / 2),
                    chartBottom - cornerRadius,
                    xCenter + (barWidth / 2),
                    chartBottom,
                    barPaint
                )
            }

            // Animacja pojawiania się liczb (wznosząca się razem ze słupkiem + alpha)
            if (total > 0) {
                valueTextPaint.color = Color.BLACK
                // Przyciemnianie z postępem animacji
                valueTextPaint.alpha = (255 * animationProgress).toInt()
                // Wartość rysowana lekko nad górną krawędzią animowanego słupka
                canvas.drawText("${total.toInt()} zł", xCenter, topY - 15f, valueTextPaint)
            }

            // Etykiety miesięcy
            canvas.drawText(label, xCenter, viewHeight - 20f, textPaint)
        }
    }

    private fun drawLineChart(canvas: Canvas, columnWidth: Float, chartBottom: Float, drawingHeight: Float, chartWidth: Float, viewHeight: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        val path = Path()
        val points = mutableListOf<Pair<Float, Float>>()

        chartData.forEachIndexed { index, pair ->
            val xCenter = yAxisSpace + (index * columnWidth) + (columnWidth / 2)
            val heightPercent = if (yAxisMax > 0) pair.second / yAxisMax else 0f
            val yCenter = chartBottom - (drawingHeight * heightPercent)
            points.add(Pair(xCenter, yCenter))

            canvas.drawText(pair.first, xCenter, viewHeight - 20f, textPaint)
        }

        val clipRight = yAxisSpace + (chartWidth * animationProgress)
        canvas.save()
        canvas.clipRect(0, 0, clipRight.toInt(), viewHeight.toInt())

        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.first, point.second)
            else path.lineTo(point.first, point.second)
        }
        canvas.drawPath(path, linePaint)

        points.forEachIndexed { index, point ->
            canvas.drawCircle(point.first, point.second, 18f, dotPaint)
            canvas.drawCircle(point.first, point.second, 9f, dotInnerPaint)

            val total = chartData[index].second
            if (total > 0) {
                valueTextPaint.color = Color.parseColor("#2196F3")
                valueTextPaint.alpha = 255
                canvas.drawText(total.toInt().toString(), point.first, point.second - 35f, valueTextPaint)
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val chartWidth = width - yAxisSpace
            val columnWidth = chartWidth / chartData.size

            if (event.x > yAxisSpace) {
                val clickedIndex = ((event.x - yAxisSpace) / columnWidth).toInt()

                if (clickedIndex in chartData.indices) {
                    val xCenter = yAxisSpace + (clickedIndex * columnWidth) + (columnWidth / 2)

                    val chartBottom = height - xAxisSpace
                    val drawingHeight = chartBottom - topSpace
                    val heightPercent = if (yAxisMax > 0) chartData[clickedIndex].second / yAxisMax else 0f
                    val yCenter = chartBottom - (drawingHeight * heightPercent)

                    if (!isBarChart) {
                        val touchRadius = 80f
                        if (Math.abs(event.x - xCenter) < touchRadius && Math.abs(event.y - yCenter) < touchRadius) {
                            onMonthClickListener?.invoke(rawMonths[clickedIndex])
                            return true
                        }
                    } else {
                        onMonthClickListener?.invoke(rawMonths[clickedIndex])
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}