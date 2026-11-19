package com.example.myfinances

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.PI

class LimitCircularChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val strokeWidthPx = 10f * density
    private val visualGapPx = 4f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E0E0E0")
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#4CAF50")
    }

    private val arcRect = RectF()
    private var currentProgress = 0f
    private var targetProgress = 0f
    private var lastCategoryKey: String? = null
    private var animator: ValueAnimator? = null

    fun setProgressData(categoryKey: String, progress: Float, color: Int, animate: Boolean) {
        val clampedTarget = progress.coerceIn(0f, 1f)
        val colorChanged = indicatorPaint.color != color
        indicatorPaint.color = color

        val isSameCategory = (lastCategoryKey == categoryKey)
        lastCategoryKey = categoryKey

        // Jeśli karta nie ma się animować (przesuwanie w bok) – ustaw wartość od razu
        if (!animate) {
            animator?.cancel()
            targetProgress = clampedTarget
            currentProgress = clampedTarget
            invalidate()
            return
        }

        if (isSameCategory && clampedTarget == targetProgress &&
            (animator?.isRunning == true || currentProgress == clampedTarget)
        ) {
            if (colorChanged) invalidate()
            return
        }

        val startValue = if (isSameCategory) currentProgress else 0f
        targetProgress = clampedTarget

        animator?.cancel()
        if (startValue == clampedTarget) {
            currentProgress = clampedTarget
            invalidate()
            return
        }

        animator = ValueAnimator.ofFloat(startValue, clampedTarget).apply {
            duration = 600L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                currentProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        // Zabezpieczenie: po odłączeniu karty w tle nie zostawiaj jej na 0f
        currentProgress = targetProgress
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        if (size <= strokeWidthPx) return

        val halfStroke = strokeWidthPx / 2f
        val left = (width - size) / 2f + halfStroke
        val top = (height - size) / 2f + halfStroke
        arcRect.set(left, top, left + size - strokeWidthPx, top + size - strokeWidthPx)

        if (currentProgress <= 0f) {
            // Brak postępu – pełne szare koło
            canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)
            return
        }

        if (currentProgress >= 1f) {
            // 100% – pełne kolorowe koło
            canvas.drawArc(arcRect, -90f, 360f, false, indicatorPaint)
            return
        }

        val activeSweep = currentProgress * 360f

        // Wyliczenie kąta przerwy (uwzględniając zaokrąglone końce obu łuków + 4dp białego odstępu)
        val radius = (size - strokeWidthPx) / 2f
        val circumference = (2.0 * PI * radius).toFloat()
        val gapAngle = ((strokeWidthPx + visualGapPx) / circumference) * 360f

        // 1. Szary tor z przerwami po obu stronach aktywnego łuku
        val trackSweep = 360f - activeSweep - (2f * gapAngle)
        if (trackSweep > 0f) {
            val trackStartAngle = -90f + activeSweep + gapAngle
            canvas.drawArc(arcRect, trackStartAngle, trackSweep, false, trackPaint)
        }

        // 2. Aktywny kolorowy łuk
        canvas.drawArc(arcRect, -90f, activeSweep, false, indicatorPaint)
    }
}