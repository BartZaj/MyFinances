package com.example.myfinances

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ExpenseDetailXMLActivity : AppCompatActivity() {

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
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        val id = intent.getStringExtra("EXTRA_ID")
        if (id.isNullOrBlank()) {
            finish()
            return
        }
        expenseId = id

        bindViews()
        setupStaticStyles()

        toolbar.setNavigationOnClickListener { finish() }

        btnEditExpense.setOnClickListener {
            currentExpense?.let { showEditDialog(it) }
        }

        btnDeleteExpense.setOnClickListener {
            currentExpense?.let { showDeleteDialog(it) }
        }

        observeExpense()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbarDetail)
        progressBar = findViewById(R.id.progressBarDetail)
        tvNotFound = findViewById(R.id.tvNotFound)
        scrollContent = findViewById(R.id.scrollContent)

        cardHero = findViewById(R.id.cardHero)
        flDetailIconBg = findViewById(R.id.flDetailIconBg)
        ivDetailCategoryIcon = findViewById(R.id.ivDetailCategoryIcon)
        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailAmount = findViewById(R.id.tvDetailAmount)

        cardInfo = findViewById(R.id.cardInfo)
        viewCategoryDot = findViewById(R.id.viewCategoryDot)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailDate = findViewById(R.id.tvDetailDate)

        btnDeleteExpense = findViewById(R.id.btnDeleteExpense)
        btnEditExpense = findViewById(R.id.btnEditExpense)
    }

    private fun setupStaticStyles() {
        cardInfo.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24)
            setColor(Color.parseColor("#F8F9FA"))
        }
    }

    private fun observeExpense() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            progressBar.visibility = View.GONE
            tvNotFound.visibility = View.VISIBLE
            return
        }

        expenseRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(uid)
            .child("Expenses")
            .child(expenseId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val amount = snapshot.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                    val dateStr = snapshot.child("date").getValue(String::class.java) ?: LocalDate.now().toString()
                    val catStr = snapshot.child("category").getValue(String::class.java) ?: ExpenseCategory.OTHER.name

                    val category = try {
                        ExpenseCategory.valueOf(catStr)
                    } catch (_: IllegalArgumentException) {
                        ExpenseCategory.OTHER
                    }

                    val expense = Expense(
                        id = expenseId,
                        name = name,
                        amount = amount,
                        date = LocalDate.parse(dateStr),
                        category = category
                    )
                    currentExpense = expense
                    renderExpense(expense)
                } else {
                    currentExpense = null
                    scrollContent.visibility = View.GONE
                    if (!isDeleting) {
                        tvNotFound.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                tvNotFound.visibility = View.VISIBLE
            }
        }

        valueEventListener = listener
        expenseRef?.addValueEventListener(listener)
    }

    private fun renderExpense(expense: Expense) {
        tvNotFound.visibility = View.GONE
        scrollContent.visibility = View.VISIBLE

        val baseColor = expense.category.color.toArgb()
        val cardBgColor = ColorUtils.setAlphaComponent(baseColor, 46) // 0.18 * 255

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
        // Podmień R.layout.dialog_add_expense na nazwę Twojego pliku z głównego ekranu
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etExpenseName)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etExpenseAmount)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etExpenseDate)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)

        var selectedDate = expense.date
        var selectedCategory = expense.category

        // Wypełnienie aktualnymi danymi wydatku
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        spinnerCategory.setAdapter(adapter)
        spinnerCategory.setText(selectedCategory.displayName, false)
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }

        etDate.setOnClickListener {
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

        val dialog = MaterialAlertDialogBuilder(this)
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
                        Toast.makeText(this, "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this, "Nie udało się zapisać zmian", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(expense: Expense) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Usuń wydatek")
            .setMessage("Czy na pewno chcesz usunąć wydatek „${expense.name}”? Tej operacji nie można cofnąć.")
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("Usuń") { _, _ ->
                isDeleting = true
                expenseRef?.removeValue()
                    ?.addOnSuccessListener {
                        Toast.makeText(this, "Usunięto wydatek", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    ?.addOnFailureListener {
                        isDeleting = false
                        Toast.makeText(this, "Nie udało się usunąć wydatku", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { expenseRef?.removeEventListener(it) }
    }
}