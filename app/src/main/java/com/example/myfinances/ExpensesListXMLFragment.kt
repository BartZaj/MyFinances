package com.example.myfinances

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpensesListFragment : Fragment() {

    private lateinit var rvExpenses: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var contentLayout: View
    private lateinit var btnFilter: MaterialButton

    private lateinit var adapter: InnerExpenseAdapter
    private var allExpenses: List<Expense> = emptyList()

    // Stany filtrów
    private var selectedCategory: ExpenseCategory? = null
    private var minAmount: Float? = null
    private var maxAmount: Float? = null
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    // Listener podpinany do MainXmlActivity
    private val dataListener: (List<Expense>) -> Unit = { expenses ->
        if (isAdded) {
            allExpenses = expenses
            progressBar.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
            // Baza przesyła nowe dane -> filtrujemy i renderujemy, ale NIE scrollujemy
            applyFiltersAndRender(scrollToTop = false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expenses_list, container, false)

        rvExpenses = view.findViewById(R.id.rvExpenses)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        contentLayout = view.findViewById(R.id.contentLayout)
        btnFilter = view.findViewById(R.id.btnFilter)

        adapter = InnerExpenseAdapter(emptyList())
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = adapter

        btnFilter.setOnClickListener {
            showFiltersBottomSheet()
        }
        updateFilterButtonText()

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

    private fun showFiltersBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val layoutInflater = LayoutInflater.from(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_filters, null)

        val llCategories = sheetView.findViewById<LinearLayout>(R.id.llCategories)
        val etMinAmount = sheetView.findViewById<TextInputEditText>(R.id.etMinAmount)
        val etMaxAmount = sheetView.findViewById<TextInputEditText>(R.id.etMaxAmount)
        val btnStartDate = sheetView.findViewById<Button>(R.id.btnStartDate)
        val btnEndDate = sheetView.findViewById<Button>(R.id.btnEndDate)
        val tvClearStartDate = sheetView.findViewById<TextView>(R.id.tvClearStartDate)
        val tvClearEndDate = sheetView.findViewById<TextView>(R.id.tvClearEndDate)
        val btnClearAll = sheetView.findViewById<MaterialButton>(R.id.btnClearAllFilters)
        val btnClose = sheetView.findViewById<MaterialButton>(R.id.btnCloseFilters)

        // Tymczasowy (roboczy) stan filtrów - nie zmienia listy w tle
        var tempCategory = selectedCategory
        var tempStartDate = startDate
        var tempEndDate = endDate

        val categoryViews = mutableMapOf<ExpenseCategory, View>()

        // A. KATEGORIE
        ExpenseCategory.entries.forEach { category ->
            val item = layoutInflater.inflate(R.layout.item_category_filter, llCategories, false)
            item.findViewById<TextView>(R.id.tvCategoryName).text = category.displayName
            item.findViewById<ImageView>(R.id.ivCategoryIcon)
                .setImageResource(categoryIconRes(category))

            item.setOnClickListener {
                tempCategory = if (tempCategory == category) null else category
                updateCategoryStyles(categoryViews, tempCategory)
            }

            llCategories.addView(item)
            categoryViews[category] = item
        }
        updateCategoryStyles(categoryViews, tempCategory)

        // B. KWOTY - tylko ustawiamy aktualne wartości, bez TextWatchera
        minAmount?.let { etMinAmount.setText(formatAmountForInput(it)) }
        maxAmount?.let { etMaxAmount.setText(formatAmountForInput(it)) }

        // C. DATY
        if (tempStartDate != null) {
            btnStartDate.text = tempStartDate!!.format(dateFormatter)
            tvClearStartDate.visibility = View.VISIBLE
        } else {
            btnStartDate.text = "Od daty"
            tvClearStartDate.visibility = View.GONE
        }

        if (tempEndDate != null) {
            btnEndDate.text = tempEndDate!!.format(dateFormatter)
            tvClearEndDate.visibility = View.VISIBLE
        } else {
            btnEndDate.text = "Do daty"
            tvClearEndDate.visibility = View.GONE
        }

        btnStartDate.setOnClickListener {
            showDatePickerDialog(tempStartDate ?: LocalDate.now()) { date ->
                tempStartDate = date
                btnStartDate.text = date.format(dateFormatter)
                tvClearStartDate.visibility = View.VISIBLE
            }
        }

        btnEndDate.setOnClickListener {
            showDatePickerDialog(tempEndDate ?: LocalDate.now()) { date ->
                tempEndDate = date
                btnEndDate.text = date.format(dateFormatter)
                tvClearEndDate.visibility = View.VISIBLE
            }
        }

        tvClearStartDate.setOnClickListener {
            tempStartDate = null
            btnStartDate.text = "Od daty"
            tvClearStartDate.visibility = View.GONE
        }

        tvClearEndDate.setOnClickListener {
            tempEndDate = null
            btnEndDate.text = "Do daty"
            tvClearEndDate.visibility = View.GONE
        }

        // D. AKCJE DOLNE
        btnClearAll.setOnClickListener {
            tempCategory = null
            tempStartDate = null
            tempEndDate = null

            etMinAmount.setText("")
            etMaxAmount.setText("")

            btnStartDate.text = "Od daty"
            tvClearStartDate.visibility = View.GONE
            btnEndDate.text = "Do daty"
            tvClearEndDate.visibility = View.GONE

            updateCategoryStyles(categoryViews, tempCategory)
        }

        btnClose.setOnClickListener {
            selectedCategory = tempCategory
            minAmount = etMinAmount.text.toString().toFloatOrNull()
            maxAmount = etMaxAmount.text.toString().toFloatOrNull()
            startDate = tempStartDate
            endDate = tempEndDate

            updateFilterButtonText()
            applyFiltersAndRender(scrollToTop = true)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(sheetView)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        bottomSheetDialog.setOnShowListener {
            val window = bottomSheetDialog.window ?: return@setOnShowListener
            WindowCompat.setDecorFitsSystemWindows(window, false)

            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows = false
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
        }

        bottomSheetDialog.show()
    }

    private fun updateFilterButtonText() {
        var activeCount = 0
        if (selectedCategory != null) activeCount++
        if (minAmount != null) activeCount++
        if (maxAmount != null) activeCount++
        if (startDate != null) activeCount++
        if (endDate != null) activeCount++

        btnFilter.text = if (activeCount > 0) "Filtruj ($activeCount)" else "Filtruj"
    }

    private fun formatAmountForInput(value: Float): String {
        return if (value % 1f == 0f) value.toInt().toString() else value.toString()
    }

    private fun showDatePickerDialog(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    private fun applyFiltersAndRender(scrollToTop: Boolean = false) {
        val currentSelectedCategory = selectedCategory
        val currentStartDate = startDate
        val currentEndDate = endDate
        val currentMin = minAmount
        val currentMax = maxAmount

        val filteredList = allExpenses.filter { expense ->
            val categoryMatch = currentSelectedCategory == null || expense.category == currentSelectedCategory
            val startDateMatch = currentStartDate == null || !expense.date.isBefore(currentStartDate)
            val endDateMatch = currentEndDate == null || !expense.date.isAfter(currentEndDate)
            val minAmountMatch = currentMin == null || expense.amount >= currentMin
            val maxAmountMatch = currentMax == null || expense.amount <= currentMax

            categoryMatch && startDateMatch && endDateMatch && minAmountMatch && maxAmountMatch
        }.sortedByDescending { it.date }

        if (filteredList.isEmpty()) {
            rvExpenses.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvExpenses.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }

        adapter.updateData(filteredList)

        if (scrollToTop && filteredList.isNotEmpty()) {
            rvExpenses.scrollToPosition(0)
        }
    }

    private fun dp(value: Int): Float = value * resources.displayMetrics.density

    private fun updateCategoryStyles(
        categoryViews: Map<ExpenseCategory, View>,
        activeCategory: ExpenseCategory?
    ) {
        categoryViews.forEach { (category, item) ->
            val isSelected = activeCategory == category
            val base = category.color.toArgb()

            (item.background.mutate() as GradientDrawable).apply {
                setColor(if (isSelected) ColorUtils.setAlphaComponent(base, 46) else Color.TRANSPARENT)
                setStroke(dp(1).toInt(), if (isSelected) Color.TRANSPARENT else Color.LTGRAY)
            }

            (item.findViewById<View>(R.id.flCategoryIcon).background.mutate() as GradientDrawable)
                .setColor(if (isSelected) base else ColorUtils.setAlphaComponent(base, 128))

            item.findViewById<TextView>(R.id.tvCategoryName).apply {
                setTextColor(if (isSelected) base else Color.GRAY)
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }
    }
}