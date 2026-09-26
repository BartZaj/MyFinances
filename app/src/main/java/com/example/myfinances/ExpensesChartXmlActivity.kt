package com.example.myfinances

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity // Ważne: AppCompatActivity dla klasycznych widoków XML
import androidx.compose.ui.graphics.toArgb // Funkcja mostu łącząca kolory Compose ze światem XML
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator

class ExpensesChartXmlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses_chart_xml)

        // 1. Przeliczenie danych (ta logika zostaje taka sama jak w Compose)
        val categoryTotals = mockExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }

        val maxTotal = categoryTotals.values.maxOrNull() ?: 0f
        val yAxisMax = if (maxTotal > 0f) {
            ((maxTotal / 100).toInt() + 1) * 100f
        } else 100f
        val stepValue = yAxisMax / 4

        // 2. Wypełnienie etykiet siatki (findViewById)
        findViewById<View>(R.id.gridLine4).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 4).toInt()}"
        findViewById<View>(R.id.gridLine3).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 3).toInt()}"
        findViewById<View>(R.id.gridLine2).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 2).toInt()}"
        findViewById<View>(R.id.gridLine1).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 1).toInt()}"
        findViewById<View>(R.id.gridLine0).findViewById<TextView>(R.id.tvYAxis).text = "0"

        val barsContainer = findViewById<LinearLayout>(R.id.barsContainer)
        val displayMetrics = resources.displayMetrics
        val cornerRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, displayMetrics)

        // 3. Dynamiczne wstrzyknięcie słupków do widoku (Inflate)
        ExpenseCategory.values().forEach { category ->
            val totalForCategory = categoryTotals[category] ?: 0f
            val heightPercentage = if (yAxisMax > 0) totalForCategory / yAxisMax else 0f

            // Ładowanie layoutu słupka
            val barView = LayoutInflater.from(this).inflate(R.layout.item_bar_chart, barsContainer, false)

            // Szukanie referencji wewnątrz konkretnego słupka
            val tvAmount = barView.findViewById<TextView>(R.id.tvAmount)
            val tvCategoryName = barView.findViewById<TextView>(R.id.tvCategoryName)
            val viewBar = barView.findViewById<View>(R.id.viewBar)
            val spacerTop = barView.findViewById<View>(R.id.spacerTop)
            val barContainer = barView.findViewById<LinearLayout>(R.id.barContainer)

            // Ustawienie tekstów
            if (totalForCategory > 0) {
                tvAmount.text = "${totalForCategory.toInt()} zł"
            }
            tvCategoryName.text = category.displayName

            // --- ZMIANA DLA ANIMACJI: Ustawienie początkowej wysokości na minimum (ukryty słupek) ---
            val barParams = viewBar.layoutParams as LinearLayout.LayoutParams
            barParams.weight = 0.01f
            viewBar.layoutParams = barParams

            val spacerParams = spacerTop.layoutParams as LinearLayout.LayoutParams
            spacerParams.weight = 0.99f
            spacerTop.layoutParams = spacerParams

            // Zaokrąglenie rogów i dodanie koloru
            val backgroundShape = GradientDrawable()
            backgroundShape.shape = GradientDrawable.RECTANGLE
            backgroundShape.setColor(category.color.toArgb()) // Magia - używamy koloru z Compose
            backgroundShape.cornerRadii = floatArrayOf(
                cornerRadiusPx, cornerRadiusPx, // Top-left
                cornerRadiusPx, cornerRadiusPx, // Top-right
                0f, 0f, 0f, 0f                  // Bottom
            )
            viewBar.background = backgroundShape

            // Dodanie interakcji! Otwieranie MainActivity
            barContainer.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            // Dopięcie gotowego widoku do ekranu
            barsContainer.addView(barView)

            // --- DODANA LOGIKA ANIMACJI W XML ---
            // Obliczamy docelową wagę, do której ma urosnąć słupek
            val targetWeight = heightPercentage.coerceAtLeast(0.01f)

            // Tworzymy animator od 0.01 do wartości docelowej
            val animator = ValueAnimator.ofFloat(0.01f, targetWeight)
            animator.duration = 1200 // Czas trwania: 1.2 sekundy
            animator.startDelay = 100 // Krótkie opóźnienie
            animator.interpolator = DecelerateInterpolator() // Zwalnia pod koniec (efekt naturalny)

            // Reakcja na każdą klatkę animacji
            animator.addUpdateListener { animation ->
                val currentWeight = animation.animatedValue as Float

                // Pobieramy obecne parametry widoków
                val currentBarParams = viewBar.layoutParams as LinearLayout.LayoutParams
                val currentSpacerParams = spacerTop.layoutParams as LinearLayout.LayoutParams

                // Aktualizujemy wagi w locie
                currentBarParams.weight = currentWeight
                currentSpacerParams.weight = 1f - currentWeight

                // Przypisujemy parametry na nowo, co wymusza przeliczenie układu (Layout Pass)
                viewBar.layoutParams = currentBarParams
                spacerTop.layoutParams = currentSpacerParams
            }

            // Startujemy animację słupka
            animator.start()
        }
    }
}