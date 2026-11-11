package com.example.myfinances

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Trace
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
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
    private lateinit var categoriesChartView: CategoriesChartView
    private lateinit var fabAddExpense: ExtendedFloatingActionButton

    private val dataListener: (List<Expense>) -> Unit = { allExpenses ->
        if (isAdded) {
            val currentMonth = LocalDate.now().monthValue
            val currentYear = LocalDate.now().year

            // Filtrowanie tylko dla tego konkretnego widoku (obecny miesiąc)
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
        categoriesChartView = view.findViewById(R.id.categoriesChartView)
        fabAddExpense = view.findViewById(R.id.fabAddExpense)

        fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        // Obsługa kliknięcia w słupek kategorii
        categoriesChartView.setOnCategoryClickListener { category ->
            val intent = Intent(requireContext(), CategoryDetailsXmlActivity::class.java).apply {
                putExtra("CATEGORY_NAME", category.name)
            }
            startActivity(intent)
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

        // 1. Zbuduj i stwórz dialog, ustawiając akcję przycisku "Dodaj" na null
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dodaj wydatek")
            .setView(dialogView)
            .setPositiveButton("Dodaj", null)
            .setNegativeButton("Wyjdź", null)
            .create()

        // 2. Pokaż dialog na ekranie
        dialog.show()

        // 3. Pobierz przycisk "Dodaj" i nadpisz jego zdarzenie kliknięcia
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text.toString()
            val amountText = etAmount.text.toString()
            val finalAmount = amountText.toFloatOrNull() ?: 0f

            val selectedCategoryName = spinnerCategory.text.toString()
            val selectedCategory =
                categories.firstOrNull { it.displayName == selectedCategoryName }
                    ?: ExpenseCategory.FOOD

            // Walidacja
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
                // Zamykamy dialog tylko, gdy dane są poprawne
                dialog.dismiss()
            } else {
                // Dane są niepoprawne – pokazujemy błąd i NIE zamykamy dialogu
                Toast.makeText(requireContext(), "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun drawChart(expenses: List<Expense>) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE

        // 1. Grupujemy wydatki z obecnego miesiąca i sumujemy ich kwoty
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }

        // 2. Mapujemy na format akceptowany przez nowy CategoriesChartView
        val chartData = ExpenseCategory.values().map { category ->
            Pair(category, categoryTotals[category] ?: 0f)
        }

        // 3. Przekazujemy dane do narysowania (widok sam zajmie się osią, animacjami i ikonami)
        categoriesChartView.setData(chartData)
    }
}