package com.example.myfinances

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.AutoCompleteTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.util.Calendar

class ExpensesChartXmlActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var barsContainer: LinearLayout
    private lateinit var fabAddExpense: ExtendedFloatingActionButton

    private var databaseRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses_chart_xml)

        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)
        barsContainer = findViewById(R.id.barsContainer)
        fabAddExpense = findViewById(R.id.fabAddExpense)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Brak zalogowanego użytkownika!", Toast.LENGTH_SHORT).show()
            return
        }

        databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        // 1. Zmiana na addValueEventListener (nasłuchiwanie na żywo)
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    mockExpenses.forEach { expense ->
                        val newExpenseRef = databaseRef!!.push()
                        val expenseData = mapOf(
                            "name" to expense.name,
                            "amount" to expense.amount,
                            "date" to expense.date.toString(),
                            "category" to expense.category.name
                        )
                        newExpenseRef.setValue(expenseData)
                    }
                    drawChart(mockExpenses)
                } else {
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
                    drawChart(fetchedExpenses)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseXML", "Błąd połączenia z bazą danych: ${error.message}")
                Toast.makeText(this@ExpensesChartXmlActivity, "Błąd wczytywania danych", Toast.LENGTH_SHORT).show()
            }
        }

        databaseRef!!.addValueEventListener(valueEventListener!!)

        // 2. Obsługa kliknięcia przycisku FAB
        fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Usuwamy nasłuchiwacz, by uniknąć wycieków pamięci
        valueEventListener?.let { databaseRef?.removeEventListener(it) }
    }

    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etExpenseName)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etExpenseAmount)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etExpenseDate)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)

        // Konfiguracja kategorii (AutoCompleteTextView jako Dropdown)
        val categories = ExpenseCategory.values()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )
        spinnerCategory.setAdapter(adapter)

        // Domyślna wartość w polu kategorii
        if (categories.isNotEmpty()) {
            spinnerCategory.setText(categories[0].displayName, false)
        }

        // Konfiguracja wyboru daty
        var selectedDate = LocalDate.now()
        etDate.setText(selectedDate.toString())

        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    etDate.setText(selectedDate.toString())
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        // Walidacja kwoty (2 miejsca po przecinku)
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()
                if (input.isNotEmpty() && !input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    etAmount.setText(input.dropLast(1))
                    etAmount.setSelection(etAmount.text?.length ?: 0)
                }
            }
        })

        // Użycie MaterialAlertDialogBuilder dla stylu Material 3
        MaterialAlertDialogBuilder(this)
            .setTitle("Dodaj wydatek")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = etName.text.toString()
                val amountText = etAmount.text.toString()
                val finalAmount = amountText.toFloatOrNull() ?: 0f

                val selectedCategoryName = spinnerCategory.text.toString()
                val selectedCategory = categories.firstOrNull { it.displayName == selectedCategoryName } ?: ExpenseCategory.FOOD

                if (name.isNotBlank() && finalAmount > 0) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val newExpenseRef = databaseRef!!.push()
                        val expenseData = mapOf(
                            "name" to name,
                            "amount" to finalAmount,
                            "date" to selectedDate.toString(),
                            "category" to selectedCategory.name
                        )
                        newExpenseRef.setValue(expenseData)
                    }
                } else {
                    Toast.makeText(this, "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Wyjdź", null)
            .show()
    }

    private fun drawChart(expenses: List<Expense>) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE

        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }

        val maxTotal = categoryTotals.values.maxOrNull() ?: 0f
        val yAxisMax = if (maxTotal > 0f) {
            ((maxTotal / 100).toInt() + 1) * 100f
        } else 100f
        val stepValue = yAxisMax / 4

        findViewById<View>(R.id.gridLine4).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 4).toInt()}"
        findViewById<View>(R.id.gridLine3).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 3).toInt()}"
        findViewById<View>(R.id.gridLine2).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 2).toInt()}"
        findViewById<View>(R.id.gridLine1).findViewById<TextView>(R.id.tvYAxis).text = "${(stepValue * 1).toInt()}"
        findViewById<View>(R.id.gridLine0).findViewById<TextView>(R.id.tvYAxis).text = "0"

        val displayMetrics = resources.displayMetrics
        val cornerRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, displayMetrics)

        barsContainer.removeAllViews()

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

            val barParams = viewBar.layoutParams as LinearLayout.LayoutParams
            barParams.weight = 0.01f
            viewBar.layoutParams = barParams

            val spacerParams = spacerTop.layoutParams as LinearLayout.LayoutParams
            spacerParams.weight = 0.99f
            spacerTop.layoutParams = spacerParams

            val backgroundShape = GradientDrawable()
            backgroundShape.shape = GradientDrawable.RECTANGLE
            backgroundShape.setColor(category.color.toArgb())
            backgroundShape.cornerRadii = floatArrayOf(
                cornerRadiusPx, cornerRadiusPx,
                cornerRadiusPx, cornerRadiusPx,
                0f, 0f, 0f, 0f
            )
            viewBar.background = backgroundShape

            barContainer.setOnClickListener {
                val intent = Intent(this@ExpensesChartXmlActivity, CategoryDetailsXmlActivity::class.java).apply {
                    putExtra("CATEGORY_NAME", category.name)
                }
                startActivity(intent)
            }

            barsContainer.addView(barView)

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