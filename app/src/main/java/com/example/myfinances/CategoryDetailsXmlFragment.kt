package com.example.myfinances

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.time.LocalDate

class CategoryDetailsXmlFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvTotalSum: TextView
    private lateinit var pieChartContainer: LinearLayout
    private lateinit var legendContainer: LinearLayout

    private var categoryName: String = ""

    private val dataListener: (List<Expense>) -> Unit = { allExpenses ->
        if (isAdded) {
            val currentMonth = LocalDate.now().monthValue
            val currentYear = LocalDate.now().year

            val filteredExpenses = allExpenses
                .filter {
                    it.category.name == categoryName &&
                            it.date.monthValue == currentMonth &&
                            it.date.year == currentYear
                }
                .sortedByDescending { it.amount }

            displayData(filteredExpenses)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_category_details_xml, container, false)

        progressBar = view.findViewById(R.id.progressBarDetails)
        contentLayout = view.findViewById(R.id.contentLayoutDetails)
        tvCategoryTitle = view.findViewById(R.id.tvCategoryTitle)
        tvTotalSum = view.findViewById(R.id.tvTotalSum)
        pieChartContainer = view.findViewById(R.id.pieChartContainer)
        legendContainer = view.findViewById(R.id.legendContainer)

        categoryName = arguments?.getString("CATEGORY_NAME").orEmpty()
        tvCategoryTitle.text = "Szczegóły: $categoryName"

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (categoryName.isBlank()) {
            findNavController().popBackStack()
            return
        }

        val mainActivity = activity as? MainXmlActivity
        if (mainActivity?.isDataLoaded == false) {
            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
        }
        mainActivity?.addDataListener(dataListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeDataListener(dataListener)
    }

    private fun generateColor(index: Int): Int {
        val hue = (index * 137.508f) % 360f
        return Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.9f))
    }

    private fun displayData(expenses: List<Expense>) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        legendContainer.removeAllViews()
        pieChartContainer.removeAllViews()

        val totalSum = expenses.sumOf { it.amount.toDouble() }.toFloat()
        tvTotalSum.text = String.format("%.2f zł", totalSum)

        val amounts = expenses.map { it.amount }
        val dynamicallyGeneratedColors = expenses.indices.map { generateColor(it) }

        val pieChartView = PieChartView(requireContext(), amounts, dynamicallyGeneratedColors)

        val sizePx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            250f,
            resources.displayMetrics
        ).toInt()

        pieChartContainer.addView(pieChartView, LinearLayout.LayoutParams(sizePx, sizePx))

        expenses.forEachIndexed { index, expense ->
            val percentage = if (totalSum > 0) ((expense.amount / totalSum) * 100).toInt() else 0
            val color = dynamicallyGeneratedColors[index]
            addLegendItem(expense, percentage, color)
        }
    }

    private fun addLegendItem(expense: Expense, percentage: Int, color: Int) {
        val ctx = requireContext()

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            gravity = Gravity.CENTER_VERTICAL

            isClickable = true
            isFocusable = true
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)

            setOnClickListener {
                findNavController().navigate(
                    R.id.expenseDetailXmlFragment,
                    bundleOf("EXTRA_ID" to expense.id),
                    slideNavOptions
                )
            }
        }

        val colorIndicator = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply { setMargins(0, 0, 32, 0) }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
            }
        }

        val tvName = TextView(ctx).apply {
            text = expense.name
            textSize = 16f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val rightCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val tvAmount = TextView(ctx).apply {
            text = String.format("- %.2f zł", expense.amount)
            setTextColor(color)
            textSize = 16f
        }

        val tvPercentage = TextView(ctx).apply {
            text = "$percentage%"
            setTextColor(Color.GRAY)
            textSize = 12f
        }

        rightCol.addView(tvAmount)
        rightCol.addView(tvPercentage)

        row.addView(colorIndicator)
        row.addView(tvName)
        row.addView(rightCol)

        legendContainer.addView(row)
    }
}

class PieChartView(context: Context, private val data: List<Float>, private val colors: List<Int>) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 120f
        strokeCap = Paint.Cap.BUTT
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val rectF = RectF()
    private var rotationAngle = 0f
    private var lastTouchAngle = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val total = data.sum()
        var currentAngle = -90f + rotationAngle
        val padding = paint.strokeWidth / 2f

        rectF.set(padding, padding, width - padding, height - padding)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (width - paint.strokeWidth) / 2f

        data.forEachIndexed { index, value ->
            val sweepAngle = if (total > 0) (value / total) * 360f else 0f
            paint.color = colors[index]
            canvas.drawArc(rectF, currentAngle, sweepAngle, false, paint)

            val percentage = if (total > 0) ((value / total) * 100).toInt() else 0

            if (percentage > 4) {
                val middleAngle = Math.toRadians((currentAngle + sweepAngle / 2).toDouble())
                val textX = cx + radius * kotlin.math.cos(middleAngle).toFloat()
                val textY = cy + radius * kotlin.math.sin(middleAngle).toFloat() - (textPaint.descent() + textPaint.ascent()) / 2

                canvas.drawText("$percentage%", textX, textY, textPaint)
            }

            currentAngle += sweepAngle
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchAngle = getAngle(event.x, event.y, cx, cy)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val currentTouchAngle = getAngle(event.x, event.y, cx, cy)
                val deltaAngle = currentTouchAngle - lastTouchAngle
                rotationAngle += deltaAngle
                lastTouchAngle = currentTouchAngle
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getAngle(x: Float, y: Float, cx: Float, cy: Float): Float {
        val angle = Math.toDegrees(kotlin.math.atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()
        return if (angle < 0) angle + 360f else angle
    }
}