package com.example.myfinances

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

class ExpensesChartXmlActivity : AppCompatActivity() {

    // Referencje do głównych elementów UI
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var barsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses_chart_xml)

        // Bindowanie widoków
        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)
        barsContainer = findViewById(R.id.barsContainer)

        // Pobranie UID z Firebase Auth
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Brak zalogowanego użytkownika!", Toast.LENGTH_SHORT).show()
            return
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        // Rozpoczęcie pobierania danych (w tym momencie ProgressBar kręci się na ekranie)
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // BAZA PUSTA: Zapisujemy mockExpenses do Firebase
                    mockExpenses.forEach { expense ->
                        val newExpenseRef = databaseRef.push()
                        val expenseData = mapOf(
                            "name" to expense.name,
                            "amount" to expense.amount,
                            "date" to expense.date.toString(),
                            "category" to expense.category.name
                        )
                        newExpenseRef.setValue(expenseData)
                    }
                    // Rysujemy wykres na podstawie mockExpenses
                    drawChart(mockExpenses)
                } else {
                    // BAZA ZAPEŁNIONA: Pobieramy i mapujemy dane
                    val fetchedExpenses = mutableListOf<Expense>()
                    for (child in snapshot.children) {
                        try {
                            val name = child.child("name").getValue(String::class.java) ?: ""
                            val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val dateStr = child.child("date").getValue(String::class.java) ?: ""
                            val categoryStr = child.child("category").getValue(String::class.java) ?: ""

                            val date = LocalDate.parse(dateStr)
                            val category = ExpenseCategory.valueOf(categoryStr)

                            fetchedExpenses.add(Expense(name, amount, date, category))
                        } catch (e: Exception) {
                            Log.e("FirebaseXML", "Błąd parsowania wydatku w XML: ${e.message}")
                        }
                    }
                    // Rysujemy wykres używając pobranych danych
                    drawChart(fetchedExpenses)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseXML", "Błąd połączenia z bazą danych: ${error.message}")
                Toast.makeText(this@ExpensesChartXmlActivity, "Błąd wczytywania danych z bazy", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funkcja wywoływana, gdy mamy już gotową listę danych
    private fun drawChart(expenses: List<Expense>) {
        // 1. Ukrywamy ProgressBar i pokazujemy właściwy layout
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE

        // 2. Przeliczanie sum w kategoriach
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }

        val maxTotal = categoryTotals.values.maxOrNull() ?: 0f
        val yAxisMax = if (maxTotal > 0f) {
            ((maxTotal / 100).toInt() + 1) * 100f
        } else 100f
        val stepValue = yAxisMax / 4

        // 3. Etykiety osi Y
        findViewById<View>(R.id.gridLine4).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 4).toInt()}"
        findViewById<View>(R.id.gridLine3).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 3).toInt()}"
        findViewById<View>(R.id.gridLine2).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 2).toInt()}"
        findViewById<View>(R.id.gridLine1).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 1).toInt()}"
        findViewById<View>(R.id.gridLine0).findViewById<TextView>(R.id.tvYAxis).text = "0"

        val displayMetrics = resources.displayMetrics
        val cornerRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, displayMetrics)

        // Oczyszczenie widoków przed ewentualnym ponownym narysowaniem
        barsContainer.removeAllViews()

        // 4. Rysowanie i animacja słupków
        ExpenseCategory.values().forEach { category ->
            val totalForCategory = categoryTotals[category] ?: 0f
            val heightPercentage = if (yAxisMax > 0) totalForCategory / yAxisMax else 0f

            val barView = LayoutInflater.from(this).inflate(R.layout.item_bar_chart, barsContainer, false)

            val tvAmount = barView.findViewById<TextView>(R.id.tvAmount)
            val tvCategoryName = barView.findViewById<TextView>(R.id.tvCategoryName)
            val viewBar = barView.findViewById<View>(R.id.viewBar)
            val spacerTop = barView.findViewById<View>(R.id.spacerTop)
            val barContainer = barView.findViewById<LinearLayout>(R.id.barContainer)

            if (totalForCategory > 0) {
                tvAmount.text = "${totalForCategory.toInt()} zł"
            }
            tvCategoryName.text = category.displayName

            // Ustawienie początkowe słupka ukrytego na 0
            val barParams = viewBar.layoutParams as LinearLayout.LayoutParams
            barParams.weight = 0.01f
            viewBar.layoutParams = barParams

            val spacerParams = spacerTop.layoutParams as LinearLayout.LayoutParams
            spacerParams.weight = 0.99f
            spacerTop.layoutParams = spacerParams

            // Dodanie kolorów i okrągłych kątów
            val backgroundShape = GradientDrawable()
            backgroundShape.shape = GradientDrawable.RECTANGLE
            backgroundShape.setColor(category.color.toArgb())
            backgroundShape.cornerRadii = floatArrayOf(
                cornerRadiusPx, cornerRadiusPx,
                cornerRadiusPx, cornerRadiusPx,
                0f, 0f, 0f, 0f
            )
            viewBar.background = backgroundShape

            // Akcja kliknięcia
            barContainer.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            barsContainer.addView(barView)

            // Logika animacji słupka
            val targetWeight = heightPercentage.coerceAtLeast(0.01f)
            val animator = ValueAnimator.ofFloat(0.01f, targetWeight)
            animator.duration = 1200
            animator.startDelay = 100
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                val currentWeight = animation.animatedValue as Float
                val currentBarParams = viewBar.layoutParams as LinearLayout.LayoutParams
                val currentSpacerParams = spacerTop.layoutParams as LinearLayout.LayoutParams

                currentBarParams.weight = currentWeight
                currentSpacerParams.weight = 1f - currentWeight

                viewBar.layoutParams = currentBarParams
                spacerTop.layoutParams = currentSpacerParams
            }

            animator.start()
        }
    }
}