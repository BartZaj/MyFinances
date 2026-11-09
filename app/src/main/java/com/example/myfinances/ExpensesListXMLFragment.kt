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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpensesListFragment : Fragment() {

    private lateinit var rvExpenses: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var contentLayout: View

    private lateinit var chipGroupCategories: ChipGroup
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
            applyFiltersAndRender() // Odśwież widok, gdy przyjda nowe dane z bazy
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
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
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
        // Używamy .entries zamiast .values() aby pozbyć się ostrzeżenia z IDE
        ExpenseCategory.entries.forEach { category ->
            // Bezpieczne tworzenie Chipa programowo
            val chip = Chip(requireContext()).apply {
                text = category.displayName
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategory = category
                        applyFiltersAndRender()
                    } else if (selectedCategory == category) {
                        selectedCategory = null
                        applyFiltersAndRender()
                    }
                }
            }
            chipGroupCategories.addView(chip)
        }

        // B. KWOTY (Używamy TextWatchera by reagować na wpisywanie z klawiatury)
        val amountWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                minAmount = etMinAmount.text.toString().toFloatOrNull()
                maxAmount = etMaxAmount.text.toString().toFloatOrNull()
                applyFiltersAndRender()
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
                applyFiltersAndRender()
            }
        }

        btnEndDate.setOnClickListener {
            showDatePickerDialog(endDate ?: LocalDate.now()) { date ->
                endDate = date
                btnEndDate.text = date.format(dateFormatter)
                tvClearEndDate.visibility = View.VISIBLE
                applyFiltersAndRender()
            }
        }

        tvClearStartDate.setOnClickListener {
            startDate = null
            btnStartDate.text = "Od daty"
            tvClearStartDate.visibility = View.GONE
            applyFiltersAndRender()
        }

        tvClearEndDate.setOnClickListener {
            endDate = null
            btnEndDate.text = "Do daty"
            tvClearEndDate.visibility = View.GONE
            applyFiltersAndRender()
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
    private fun applyFiltersAndRender() {
        // Przypisanie do niemutowalnych stałych lokalnych rozwiązuje błąd Smart Cast
        val currentSelectedCategory = selectedCategory
        val currentStartDate = startDate
        val currentEndDate = endDate
        val currentMin = minAmount
        val currentMax = maxAmount

        val filteredList = allExpenses.filter { expense ->
            val categoryMatch = currentSelectedCategory == null || expense.category == currentSelectedCategory

            // isBefore() i isAfter() pilnują krawędzi (np. wydatki z samego początku/końca zakresu)
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
    }
}