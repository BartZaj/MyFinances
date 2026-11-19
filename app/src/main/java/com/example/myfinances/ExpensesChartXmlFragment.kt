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
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.util.Calendar

class ExpensesChartFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var categoriesChartView: CategoriesChartView
    private lateinit var fabAddExpense: ExtendedFloatingActionButton
    private lateinit var vpCategoryLimits: ViewPager2
    private lateinit var limitAdapter: CategoryLimitAdapter

    // Stan danych z obecnego miesiąca oraz limitów z Firebase
    private var currentMonthExpenses: List<Expense> = emptyList()
    private var categoryLimits: Map<ExpenseCategory, Float> = emptyMap()

    private var limitsRef: DatabaseReference? = null
    private var limitsListener: ValueEventListener? = null

    private val dataListener: (List<Expense>) -> Unit = { allExpenses ->
        if (isAdded) {
            val currentMonth = LocalDate.now().monthValue
            val currentYear = LocalDate.now().year

            currentMonthExpenses = allExpenses.filter {
                it.date.monthValue == currentMonth && it.date.year == currentYear
            }

            drawChart(currentMonthExpenses)
            updateLimitsPager()
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
        vpCategoryLimits = view.findViewById(R.id.vpCategoryLimits)

        // Konfiguracja ViewPager2 dla kart limitów
        limitAdapter = CategoryLimitAdapter(
            currentPageProvider = { vpCategoryLimits.currentItem },
            onEditClick = { category, currentLimit ->
                showEditLimitBottomSheet(category, currentLimit)
            }
        )
        vpCategoryLimits.adapter = limitAdapter
        vpCategoryLimits.getChildAt(0)?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            defaultFocusHighlightEnabled = false
        }

        fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

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
        observeLimitsFromFirebase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeDataListener(dataListener)
        limitsListener?.let { limitsRef?.removeEventListener(it) }
    }

    private fun observeLimitsFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        limitsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Limits")

        limitsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedLimits = mutableMapOf<ExpenseCategory, Float>()
                for (child in snapshot.children) {
                    val catName = child.key ?: continue
                    val limitVal = child.getValue(Double::class.java)?.toFloat() ?: 0f
                    try {
                        fetchedLimits[ExpenseCategory.valueOf(catName)] = limitVal
                    } catch (_: IllegalArgumentException) {
                        // Ignoruj nieznane klucze
                    }
                }
                categoryLimits = fetchedLimits
                if (isAdded) {
                    updateLimitsPager()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        limitsRef?.addValueEventListener(limitsListener!!)
    }

    private fun updateLimitsPager() {
        val categoryTotals = currentMonthExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }

        val items = ExpenseCategory.values().map { category ->
            CategoryLimitItem(
                category = category,
                spent = categoryTotals[category] ?: 0f,
                limit = categoryLimits[category] ?: 0f
            )
        }
        limitAdapter.submitList(items)
    }

    private fun showEditLimitBottomSheet(category: ExpenseCategory, currentLimit: Float) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_limit, null)

        val tvTitle = sheetView.findViewById<TextView>(R.id.tvBottomSheetTitle)
        val etLimitAmount = sheetView.findViewById<TextInputEditText>(R.id.etLimitAmount)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelLimit)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveLimit)

        tvTitle.text = "Limit: ${category.displayName}"
        if (currentLimit > 0f) {
            val limitText = currentLimit.toInt().toString()
            etLimitAmount.setText(limitText)
            etLimitAmount.setSelection(limitText.length)
        }

        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }

        btnSave.setOnClickListener {
            val newLimit = etLimitAmount.text.toString().toFloatOrNull()
            if (newLimit != null && newLimit >= 0f) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(uid)
                        .child("Limits")
                        .child(category.name)
                        .setValue(newLimit.toDouble())
                }
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Wprowadź poprawną kwotę", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setContentView(sheetView)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        bottomSheetDialog.setOnShowListener {
            val window = bottomSheetDialog.window ?: return@setOnShowListener
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Wyłączamy domyślne wcięcia na kontenerach Material, żeby nie szarpały okna
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows = false
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
        }

        bottomSheetDialog.show()
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dodaj wydatek")
            .setView(dialogView)
            .setPositiveButton("Dodaj", null)
            .setNegativeButton("Wyjdź", null)
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun drawChart(expenses: List<Expense>) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE

        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }

        val chartData = ExpenseCategory.values().map { category ->
            Pair(category, categoryTotals[category] ?: 0f)
        }

        categoriesChartView.setData(chartData)
    }
}