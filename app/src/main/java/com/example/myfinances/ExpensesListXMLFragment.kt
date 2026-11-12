package com.example.myfinances

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import android.widget.ImageView
import android.widget.LinearLayout

class ExpensesListFragment : Fragment() {

    private lateinit var rvExpenses: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var contentLayout: View

    private lateinit var llCategories: LinearLayout
    private val categoryViews = mutableMapOf<ExpenseCategory, View>()
    private lateinit var etMinAmount: TextInputEditText
    private lateinit var etMaxAmount: TextInputEditText
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var tvClearStartDate: TextView
    private lateinit var tvClearEndDate: TextView

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

        // Bindowanie widoków
        rvExpenses = view.findViewById(R.id.rvExpenses)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        contentLayout = view.findViewById(R.id.contentLayout)
        llCategories = view.findViewById(R.id.llCategories)
        etMinAmount = view.findViewById(R.id.etMinAmount)
        etMaxAmount = view.findViewById(R.id.etMaxAmount)
        btnStartDate = view.findViewById(R.id.btnStartDate)
        btnEndDate = view.findViewById(R.id.btnEndDate)
        tvClearStartDate = view.findViewById(R.id.tvClearStartDate)
        tvClearEndDate = view.findViewById(R.id.tvClearEndDate)

        // Konfiguracja RecyclerView (kluczowe dla pamięci przy scrollowaniu)
        adapter = InnerExpenseAdapter(emptyList()) // Używamy adaptera, który stworzyłeś wcześniej
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = adapter

        setupFilters()

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

    private fun setupFilters() {
        // A. KATEGORIE
        val layoutInflater = LayoutInflater.from(requireContext())
        ExpenseCategory.entries.forEach { category ->
            val item = layoutInflater.inflate(R.layout.item_category_filter, llCategories, false)
            item.findViewById<TextView>(R.id.tvCategoryName).text = category.displayName
            item.findViewById<ImageView>(R.id.ivCategoryIcon)
                .setImageResource(categoryIconRes(category))

            item.setOnClickListener {
                // Ponowne kliknięcie zaznaczonej kategorii ją odznacza
                selectedCategory = if (selectedCategory == category) null else category
                updateCategoryStyles()
                applyFiltersAndRender(scrollToTop = true)
            }

            llCategories.addView(item)
            categoryViews[category] = item
        }
        updateCategoryStyles()

        // B. KWOTY
        val amountWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                minAmount = etMinAmount.text.toString().toFloatOrNull()
                maxAmount = etMaxAmount.text.toString().toFloatOrNull()
                applyFiltersAndRender(scrollToTop = true) // <--- Zmiana
            }
        }
        etMinAmount.addTextChangedListener(amountWatcher)
        etMaxAmount.addTextChangedListener(amountWatcher)

        // C. DATY
        btnStartDate.setOnClickListener {
            showDatePickerDialog(startDate ?: LocalDate.now()) { date ->
                startDate = date
                btnStartDate.text = date.format(dateFormatter)
                tvClearStartDate.visibility = View.VISIBLE
                applyFiltersAndRender(scrollToTop = true) // <--- Zmiana
            }
        }

        btnEndDate.setOnClickListener {
            showDatePickerDialog(endDate ?: LocalDate.now()) { date ->
                endDate = date
                btnEndDate.text = date.format(dateFormatter)
                tvClearEndDate.visibility = View.VISIBLE
                applyFiltersAndRender(scrollToTop = true) // <--- Zmiana
            }
        }

        tvClearStartDate.setOnClickListener {
            startDate = null
            btnStartDate.text = "Od daty"
            tvClearStartDate.visibility = View.GONE
            applyFiltersAndRender(scrollToTop = true) // <--- Zmiana
        }

        tvClearEndDate.setOnClickListener {
            endDate = null
            btnEndDate.text = "Do daty"
            tvClearEndDate.visibility = View.GONE
            applyFiltersAndRender(scrollToTop = true) // <--- Zmiana
        }
    }

    private fun showDatePickerDialog(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1, // Kalendarz liczy miesiące od 0
            initialDate.dayOfMonth
        ).show()
    }

    // =========================================================
    // OPTYMALIZACJA PAMIĘCIOWA (Identyczna z pamięcią w Compose)
    // =========================================================
    // Ta metoda wykonuje przeliczenia (filtrowanie i sortowanie) TYLKO WTEDY,
    // kiedy zmienią się filtry (kliknięcie usera) lub gdy przyjdą nowe dane z bazy (logowanie).
    // Podczas "scrollowania palcem w dół" ta metoda całkowicie milczy.
// Zmieniona sygnatura - domyślnie NIE scrolluje (przydatne przy aktualizacji z bazy)
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

        // DODANE: Sprawdza flagę, zanim przewinie na górę
        if (scrollToTop && filteredList.isNotEmpty()) {
            rvExpenses.scrollToPosition(0)
        }
    }

    private fun dp(value: Int): Float = value * resources.displayMetrics.density

    // Wygląd chipa jak w Composie: po zaznaczeniu kolorowe tło (alpha 0.18), pogrubiony tekst,
    // przed zaznaczeniem szara ramka i szary tekst
    // Wygląd jak Box w Composie: zaznaczona kategoria ma kolorowe tło (alpha 0.18),
    // niezaznaczona szarą ramkę i szary tekst
    private fun updateCategoryStyles() {
        categoryViews.forEach { (category, item) ->
            val isSelected = selectedCategory == category
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