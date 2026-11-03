package com.example.myfinances

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Trace
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.util.Calendar

class ExpensesChartFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var barsContainer: LinearLayout
    private lateinit var fabAddExpense: ExtendedFloatingActionButton

    private val animators = mutableListOf<ValueAnimator>()
    private val categoryViews = mutableMapOf<ExpenseCategory, View>()

    private val dataListener: (List<Expense>) -> Unit = { allExpenses ->
        if (isAdded) {
            val currentMonth = LocalDate.now().monthValue
            val currentYear = LocalDate.now().year

            // Filtrowanie tylko dla tego konkretnego widoku
            val currentMonthExpenses = allExpenses.filter {
                it.date.monthValue == currentMonth && it.date.year == currentYear
            }

            drawChart(currentMonthExpenses)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Trace.beginSection("XMLScreen_ExpensesChart")
        val view = inflater.inflate(R.layout.activity_expenses_chart_xml, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        contentLayout = view.findViewById(R.id.contentLayout)
        barsContainer = view.findViewById(R.id.barsContainer)
        fabAddExpense = view.findViewById(R.id.fabAddExpense)

        fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        Trace.endSection()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        animators.forEach { it.cancel() }
        animators.clear()

        // DODAJ TĘ LINIJKĘ - czyści stare widoki przy wyjściu z fragmentu
        categoryViews.clear()
    }

    private fun showAddExpenseDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etExpenseName)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etExpenseAmount)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etExpenseDate)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)

        val categories = ExpenseCategory.values()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )
        spinnerCategory.setAdapter(adapter)

        if (categories.isNotEmpty()) {
            spinnerCategory.setText(categories[0].displayName, false)
        }

        var selectedDate = LocalDate.now()
        etDate.setText(selectedDate.toString())

        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    etDate.setText(selectedDate.toString())
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dodaj wydatek")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = etName.text.toString()
                val amountText = etAmount.text.toString()
                val finalAmount = amountText.toFloatOrNull() ?: 0f

                val selectedCategoryName = spinnerCategory.text.toString()
                val selectedCategory =
                    categories.firstOrNull { it.displayName == selectedCategoryName }
                        ?: ExpenseCategory.FOOD

                if (name.isNotBlank() && finalAmount > 0) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val databaseRef =
                            FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                .child("Expenses")
                        val newExpenseRef = databaseRef.push()
                        val expenseData = mapOf(
                            "name" to name,
                            "amount" to finalAmount,
                            "date" to selectedDate.toString(),
                            "category" to selectedCategory.name
                        )
                        newExpenseRef.setValue(expenseData)
                    }
                } else {
                    Toast.makeText(requireContext(), "Wprowadź poprawne dane", Toast.LENGTH_SHORT)
                        .show()
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

        view?.findViewById<View>(R.id.gridLine4)?.findViewById<TextView>(R.id.tvYAxis)?.text =
            "${(stepValue * 4).toInt()}"
        view?.findViewById<View>(R.id.gridLine3)?.findViewById<TextView>(R.id.tvYAxis)?.text =
            "${(stepValue * 3).toInt()}"
        view?.findViewById<View>(R.id.gridLine2)?.findViewById<TextView>(R.id.tvYAxis)?.text =
            "${(stepValue * 2).toInt()}"
        view?.findViewById<View>(R.id.gridLine1)?.findViewById<TextView>(R.id.tvYAxis)?.text =
            "${(stepValue * 1).toInt()}"
        view?.findViewById<View>(R.id.gridLine0)?.findViewById<TextView>(R.id.tvYAxis)?.text = "0"

        val displayMetrics = resources.displayMetrics
        val cornerRadiusPx =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, displayMetrics)

        animators.forEach { it.cancel() }
        animators.clear()

        // 1. Tworzymy widoki słupków tylko jeśli jeszcze nie istnieją
        if (categoryViews.isEmpty()) {
            barsContainer.removeAllViews()

            ExpenseCategory.values().forEach { category ->
                val barView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_bar_chart, barsContainer, false)

                val tvCategoryName = barView.findViewById<TextView>(R.id.tvCategoryName)
                val viewBar = barView.findViewById<View>(R.id.viewBar)
                val barContainer = barView.findViewById<LinearLayout>(R.id.barContainer)

                tvCategoryName.text = category.displayName

                // Ustawienie początkowej wagi (wysokości)
                val barParams = viewBar.layoutParams as LinearLayout.LayoutParams
                barParams.weight = 0.01f
                viewBar.layoutParams = barParams

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
                    val intent =
                        Intent(requireContext(), CategoryDetailsXmlActivity::class.java).apply {
                            putExtra("CATEGORY_NAME", category.name)
                        }
                    startActivity(intent)
                }

                barsContainer.addView(barView)
                categoryViews[category] = barView // Zapisujemy widok do pamięci
            }
        }

        // 2. Aktualizujemy wartości i animujemy od OBECNEJ wysokości
        ExpenseCategory.values().forEach { category ->
            val totalForCategory = categoryTotals[category] ?: 0f
            val heightPercentage = if (yAxisMax > 0) totalForCategory / yAxisMax else 0f
            val targetWeight = heightPercentage.coerceAtLeast(0.01f)

            val barView = categoryViews[category] ?: return@forEach
            val tvAmount = barView.findViewById<TextView>(R.id.tvAmount)
            val viewBar = barView.findViewById<View>(R.id.viewBar)
            val spacerTop = barView.findViewById<View>(R.id.spacerTop)

            if (totalForCategory > 0) {
                tvAmount.text = "${totalForCategory.toInt()} zł"
            } else {
                tvAmount.text = "" // Ukrywamy tekst jeśli wartość spadła do 0
            }

            val currentBarParams = viewBar.layoutParams as LinearLayout.LayoutParams
            val currentWeight = currentBarParams.weight

            // Animujemy tylko wtedy, gdy waga (wysokość) faktycznie uległa zmianie
            if (currentWeight != targetWeight) {
                val animator = ValueAnimator.ofFloat(currentWeight, targetWeight)
                animator.duration = 1200
                animator.startDelay = 100
                animator.interpolator = DecelerateInterpolator()

                animator.addUpdateListener { animation ->
                    val currentAnimatedWeight = animation.animatedValue as Float
                    val animatedBarParams = viewBar.layoutParams as LinearLayout.LayoutParams
                    val animatedSpacerParams = spacerTop.layoutParams as LinearLayout.LayoutParams

                    animatedBarParams.weight = currentAnimatedWeight
                    animatedSpacerParams.weight = 1f - currentAnimatedWeight

                    viewBar.layoutParams = animatedBarParams
                    spacerTop.layoutParams = animatedSpacerParams
                }

                animators.add(animator)
                animator.start()
            }
        }
    }
}