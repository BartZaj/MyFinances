package com.example.myfinances

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseDetailXmlFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNotFound: TextView
    private lateinit var scrollContent: ScrollView

    private lateinit var cardHero: LinearLayout
    private lateinit var flDetailIconBg: FrameLayout
    private lateinit var ivDetailCategoryIcon: ImageView
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailAmount: TextView

    private lateinit var cardInfo: LinearLayout
    private lateinit var viewCategoryDot: View
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailDate: TextView

    private lateinit var btnDeleteExpense: MaterialButton
    private lateinit var btnEditExpense: MaterialButton

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private var expenseId: String = ""
    private var currentExpense: Expense? = null
    private var isDeleting = false

    private var expenseRef: DatabaseReference? = null

    private val dataListener: (List<Expense>) -> Unit = { allExpenses ->
        if (isAdded) {
            progressBar.visibility = View.GONE
            val found = allExpenses.find { it.id == expenseId }
            if (found != null) {
                currentExpense = found
                renderExpense(found)
            } else {
                currentExpense = null
                scrollContent.visibility = View.GONE
                if (!isDeleting) {
                    tvNotFound.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_expense_detail, container, false)
        bindViews(view)
        setupStaticStyles()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = arguments?.getString("EXTRA_ID")
        if (id.isNullOrBlank()) {
            findNavController().popBackStack()
            return
        }
        expenseId = id

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            expenseRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("Expenses")
                .child(expenseId)
        }

        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        btnEditExpense.setOnClickListener {
            currentExpense?.let { showEditDialog(it) }
        }

        btnDeleteExpense.setOnClickListener {
            currentExpense?.let { showDeleteDialog(it) }
        }

        val mainActivity = activity as? MainXmlActivity
        if (mainActivity?.isDataLoaded == false) {
            progressBar.visibility = View.VISIBLE
            scrollContent.visibility = View.GONE
        }
        mainActivity?.addDataListener(dataListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeDataListener(dataListener)
    }

    private fun bindViews(view: View) {
        toolbar = view.findViewById(R.id.toolbarDetail)
        progressBar = view.findViewById(R.id.progressBarDetail)
        tvNotFound = view.findViewById(R.id.tvNotFound)
        scrollContent = view.findViewById(R.id.scrollContent)

        cardHero = view.findViewById(R.id.cardHero)
        flDetailIconBg = view.findViewById(R.id.flDetailIconBg)
        ivDetailCategoryIcon = view.findViewById(R.id.ivDetailCategoryIcon)
        tvDetailName = view.findViewById(R.id.tvDetailName)
        tvDetailAmount = view.findViewById(R.id.tvDetailAmount)

        cardInfo = view.findViewById(R.id.cardInfo)
        viewCategoryDot = view.findViewById(R.id.viewCategoryDot)
        tvDetailCategory = view.findViewById(R.id.tvDetailCategory)
        tvDetailDate = view.findViewById(R.id.tvDetailDate)

        btnDeleteExpense = view.findViewById(R.id.btnDeleteExpense)
        btnEditExpense = view.findViewById(R.id.btnEditExpense)
    }

    private fun setupStaticStyles() {
        cardInfo.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24)
            setColor(Color.parseColor("#F8F9FA"))
        }
    }

    private fun renderExpense(expense: Expense) {
        tvNotFound.visibility = View.GONE
        scrollContent.visibility = View.VISIBLE

        val baseColor = expense.category.color.toArgb()
        val cardBgColor = ColorUtils.setAlphaComponent(baseColor, 46)

        cardHero.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24)
            setColor(cardBgColor)
        }

        flDetailIconBg.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(baseColor)
        }

        viewCategoryDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(baseColor)
        }

        ivDetailCategoryIcon.setImageResource(categoryIconRes(expense.category))

        tvDetailName.text = expense.name
        tvDetailAmount.text = String.format("- %.2f zł", expense.amount)
        tvDetailAmount.setTextColor(baseColor)

        tvDetailCategory.text = expense.category.displayName
        tvDetailCategory.setTextColor(baseColor)

        tvDetailDate.text = expense.date.format(dateFormatter)

        btnEditExpense.backgroundTintList = ColorStateList.valueOf(baseColor)
    }

    private fun showEditDialog(expense: Expense) {
        val ctx = requireContext()
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_expense, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etExpenseName)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etExpenseAmount)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etExpenseDate)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)

        var selectedDate = expense.date
        var selectedCategory = expense.category

        etName.setText(expense.name)
        val formattedAmount = if (expense.amount % 1f == 0f) {
            expense.amount.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", expense.amount)
        }
        etAmount.setText(formattedAmount)
        etDate.setText(selectedDate.toString())

        val categories = ExpenseCategory.values()
        val categoryNames = categories.map { it.displayName }
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, categoryNames)
        spinnerCategory.setAdapter(adapter)
        spinnerCategory.setText(selectedCategory.displayName, false)
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }

        etDate.setOnClickListener {
            DatePickerDialog(
                ctx,
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    etDate.setText(selectedDate.toString())
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Edytuj wydatek")
            .setView(dialogView)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Zapisz", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = etName.text?.toString()?.trim().orEmpty()
                val amountRaw = etAmount.text?.toString()?.trim()?.replace(',', '.').orEmpty()
                val newAmount = amountRaw.toFloatOrNull()

                if (newName.isBlank()) {
                    etName.error = "Podaj nazwę"
                    return@setOnClickListener
                }
                if (newAmount == null || newAmount <= 0f) {
                    etAmount.error = "Podaj poprawną kwotę"
                    return@setOnClickListener
                }

                val updatedData = mapOf(
                    "name" to newName,
                    "amount" to newAmount.toDouble(),
                    "date" to selectedDate.toString(),
                    "category" to selectedCategory.name
                )

                expenseRef?.updateChildren(updatedData)
                    ?.addOnSuccessListener {
                        context?.let { Toast.makeText(it, "Zapisano zmiany", Toast.LENGTH_SHORT).show() }
                        dialog.dismiss()
                    }
                    ?.addOnFailureListener {
                        context?.let { Toast.makeText(it, "Nie udało się zapisać zmian", Toast.LENGTH_SHORT).show() }
                    }
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(expense: Expense) {
        val ctx = requireContext()
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Usuń wydatek")
            .setMessage("Czy na pewno chcesz usunąć wydatek „${expense.name}”? Tej operacji nie można cofnąć.")
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Usuń") { _, _ ->
                isDeleting = true
                expenseRef?.removeValue()
                    ?.addOnSuccessListener {
                        context?.let { Toast.makeText(it, "Usunięto wydatek", Toast.LENGTH_SHORT).show() }
                        if (isAdded) {
                            findNavController().popBackStack()
                        }
                    }
                    ?.addOnFailureListener {
                        isDeleting = false
                        context?.let { Toast.makeText(it, "Nie udało się usunąć wydatku", Toast.LENGTH_SHORT).show() }
                    }
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#D32F2F"))
        }

        dialog.show()
    }

    private fun dp(value: Int): Float = value * resources.displayMetrics.density
}