package com.example.myfinances

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate

class CategoryDetailsXmlActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvTotalSum: TextView
    private lateinit var pieChartContainer: LinearLayout
    private lateinit var legendContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_details_xml)

        progressBar = findViewById(R.id.progressBarDetails)
        contentLayout = findViewById(R.id.contentLayoutDetails)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        tvTotalSum = findViewById(R.id.tvTotalSum)
        pieChartContainer = findViewById(R.id.pieChartContainer)
        legendContainer = findViewById(R.id.legendContainer)

        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: return
        tvCategoryTitle.text = "Szczegóły: $categoryName"

        loadCategoryData(categoryName)
    }

    private fun loadCategoryData(categoryName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentMonth = LocalDate.now().monthValue
                val currentYear = LocalDate.now().year

                val fetchedExpenses = mutableListOf<Expense>()
                for (child in snapshot.children) {
                    val catStr = child.child("category").getValue(String::class.java) ?: ""

                    if (catStr == categoryName) {
                        val id = child.key ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                        val dateStr = child.child("date").getValue(String::class.java) ?: LocalDate.now().toString()

                        val date = LocalDate.parse(dateStr)

                        // DODANY WARUNEK: Tylko wydatki z bieżącego miesiąca i roku
                        if (date.monthValue == currentMonth && date.year == currentYear) {
                            fetchedExpenses.add(Expense(id, name, amount, date, ExpenseCategory.valueOf(catStr)))
                        }
                    }
                }

                fetchedExpenses.sortByDescending { it.amount }
                displayData(fetchedExpenses)
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
            }
        })
    }

    // Funkcja generująca nieskończoną ilość unikalnych kolorów
    private fun generateColor(index: Int): Int {
        // Użycie Złotego Kąta (137.5 stopni) gwarantuje, że każdy kolejny kolor
        // jest przesunięty na kole barw w sposób minimalizujący nakładanie się.
        val hue = (index * 137.508f) % 360f
        // Nasycenie (0.7) i Jasność (0.9) - dają ładne, żywe, "pastelowe" odcienie
        return Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.9f))
    }

    private fun displayData(expenses: List<Expense>) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        legendContainer.removeAllViews()
        pieChartContainer.removeAllViews()

        val totalSum = expenses.sumOf { it.amount.toDouble() }.toFloat()
        // Kwota do dwóch miejsc po przecinku
        tvTotalSum.text = String.format("%.2f zł", totalSum)

        val amounts = expenses.map { it.amount }
        val dynamicallyGeneratedColors = expenses.indices.map { generateColor(it) }

        val pieChartView = PieChartView(this, amounts, dynamicallyGeneratedColors)

        val sizePx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            250f,
            resources.displayMetrics
        ).toInt()

        pieChartContainer.addView(pieChartView, LinearLayout.LayoutParams(sizePx, sizePx))

        expenses.forEachIndexed { index, expense ->
            val percentage = if (totalSum > 0) ((expense.amount / totalSum) * 100).toInt() else 0
            val color = dynamicallyGeneratedColors[index]
            // TU BYŁ BŁĄD: Przekazujemy cały obiekt 'expense', a nie tylko jego nazwę i kwotę
            addLegendItem(expense, percentage, color)
        }
    }

    // TU BYŁ BŁĄD: Przyjmujemy obiekt Expense jako argument
    private fun addLegendItem(expense: Expense, percentage: Int, color: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 16)
            }
            gravity = Gravity.CENTER_VERTICAL

            // TU BYŁ BŁĄD: Te linie muszą być wewnątrz bloku .apply {} obiektu row!
            isClickable = true
            isFocusable = true
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)

            setOnClickListener {
                val intent = Intent(this@CategoryDetailsXmlActivity, ExpenseDetailXMLActivity::class.java).apply {
                    putExtra("EXTRA_ID", expense.id)
                    putExtra("EXTRA_NAME", expense.name)
                    putExtra("EXTRA_AMOUNT", expense.amount)
                    putExtra("EXTRA_DATE", expense.date.toString())
                    putExtra("EXTRA_CATEGORY", expense.category.name)
                }
                startActivity(intent)
            }
        } // <- Tutaj zamyka się blok apply dla obiektu row

        val colorIndicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply { setMargins(0, 0, 32, 0) }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
            }
        }

        val tvName = TextView(this).apply {
            text = expense.name
            textSize = 16f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val rightCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val tvAmount = TextView(this).apply {
            // TU BYŁ BŁĄD: Formatowanie do 2 miejsc po przecinku
            text = String.format("- %.2f zł", expense.amount)
            setTextColor(color)
            textSize = 16f
        }

        val tvPercentage = TextView(this).apply {
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

        // UWAGA: Poprawiony błąd z Twojego kodu (brakowało minusa)
        rectF.set(padding, padding, width - padding, height - padding)

        val cx = width / 2f
        val cy = height / 2f
        // UWAGA: Poprawiony błąd (brakowało minusa)
        val radius = (width - paint.strokeWidth) / 2f

        data.forEachIndexed { index, value ->
            val sweepAngle = if (total > 0) (value / total) * 360f else 0f
            // Używamy koloru bezpośrednio, ponieważ teraz obydwie listy mają tę samą wielkość
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