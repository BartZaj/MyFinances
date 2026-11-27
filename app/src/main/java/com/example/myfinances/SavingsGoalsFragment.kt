package com.example.myfinances

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate

class SavingsGoalsFragment : Fragment() {

    private lateinit var rvGoals: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var btnAddGoal: MaterialButton
    private lateinit var adapter: SavingsGoalAdapter

    private var shouldScrollToTop = false

    private val goalsListener: (List<SavingsGoal>) -> Unit = { goals ->
        if (isAdded) {
            val sortedGoals = goals.sortedWith(
                compareBy<SavingsGoal> { it.isCompleted }
                    .thenByDescending { it.id }
            )

            progressBar.visibility = View.GONE

            if (sortedGoals.isEmpty()) {
                rvGoals.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
            } else {
                rvGoals.visibility = View.VISIBLE
                tvEmptyState.visibility = View.GONE
            }

            adapter.submitList(sortedGoals) {
                if (shouldScrollToTop && sortedGoals.isNotEmpty()) {
                    rvGoals.scrollToPosition(0)
                    shouldScrollToTop = false
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_savings_goals, container, false)

        rvGoals = view.findViewById(R.id.rvGoals)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnAddGoal = view.findViewById(R.id.btnAddGoal)

        adapter = SavingsGoalAdapter(
            onAddFundsClick = { goal -> showAddFundsBottomSheet(goal) },
            onAddAsExpenseClick = { goal -> convertGoalToExpense(goal) },
            onDeleteClick = { goal -> deleteGoal(goal) }
        )

        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        rvGoals.adapter = adapter
        (rvGoals.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        btnAddGoal.setOnClickListener {
            showAddGoalDialog()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainXmlActivity
        if (mainActivity?.isGoalsLoaded == false) {
            progressBar.visibility = View.VISIBLE
            rvGoals.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        }

        mainActivity?.addGoalsListener(goalsListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeGoalsListener(goalsListener)
    }

    private fun showAddGoalDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_savings_goal, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etGoalName)
        val etTargetAmount = dialogView.findViewById<TextInputEditText>(R.id.etGoalTargetAmount)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerGoalCategory)

        val categories = ExpenseCategory.values()
        val arrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )
        spinnerCategory.setAdapter(arrayAdapter)
        if (categories.isNotEmpty()) {
            spinnerCategory.setText(categories[0].displayName, false)
        }

        attachDecimalValidator(etTargetAmount)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dodaj cel oszczędnościowy")
            .setView(dialogView)
            .setPositiveButton("Dodaj", null)
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text.toString().trim()
            val targetAmount = etTargetAmount.text.toString().toFloatOrNull() ?: 0f
            val selectedCategoryName = spinnerCategory.text.toString()
            val selectedCategory = categories.firstOrNull { it.displayName == selectedCategoryName }
                ?: ExpenseCategory.FOOD

            if (name.isNotBlank() && targetAmount > 0f) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    shouldScrollToTop = true
                    val newGoalRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(uid)
                        .child("SavingsGoals")
                        .push()

                    val goalData = mapOf(
                        "name" to name,
                        "targetAmount" to targetAmount.toDouble(),
                        "currentAmount" to 0.0,
                        "category" to selectedCategory.name,
                        "completed" to false
                    )
                    newGoalRef.setValue(goalData)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddFundsBottomSheet(goal: SavingsGoal) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_savings_amount, null)

        val tvTitle = sheetView.findViewById<TextView>(R.id.tvBottomSheetTitle)
        val etAddAmount = sheetView.findViewById<TextInputEditText>(R.id.etAddAmount)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelAddAmount)
        val btnConfirm = sheetView.findViewById<MaterialButton>(R.id.btnConfirmAddAmount)

        tvTitle.text = "Dodaj do: ${goal.name}"
        attachDecimalValidator(etAddAmount)

        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }

        btnConfirm.setOnClickListener {
            val addedAmount = etAddAmount.text.toString().toFloatOrNull()
            if (addedAmount != null && addedAmount > 0f) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val newTotal = (goal.currentAmount + addedAmount).toDouble()
                    FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(uid)
                        .child("SavingsGoals")
                        .child(goal.id)
                        .child("currentAmount")
                        .setValue(newTotal)
                }
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Wprowadź poprawną kwotę", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setContentView(sheetView)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        val activityWindow = requireActivity().window
        val originalSoftInputMode = activityWindow.attributes.softInputMode
        activityWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        bottomSheetDialog.setOnShowListener {
            val window = bottomSheetDialog.window ?: return@setOnShowListener
            WindowCompat.setDecorFitsSystemWindows(window, false)

            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows = false
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false

            bottomSheetDialog.setOnDismissListener {
                activity?.window?.setSoftInputMode(originalSoftInputMode)
            }
        }

        bottomSheetDialog.show()
    }

    private fun convertGoalToExpense(goal: SavingsGoal) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        val expenseData = mapOf(
            "name" to goal.name,
            "amount" to goal.currentAmount,
            "date" to LocalDate.now().toString(),
            "category" to goal.category.name
        )
        userRef.child("Expenses").push().setValue(expenseData)

        userRef.child("SavingsGoals")
            .child(goal.id)
            .child("completed")
            .setValue(true)

        Toast.makeText(requireContext(), "Dodano jako wydatek", Toast.LENGTH_SHORT).show()
    }

    private fun deleteGoal(goal: SavingsGoal) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(uid)
            .child("SavingsGoals")
            .child(goal.id)
            .removeValue()
    }

    private fun attachDecimalValidator(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()
                if (input.isNotEmpty() && !input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    editText.setText(input.dropLast(1))
                    editText.setSelection(editText.text?.length ?: 0)
                }
            }
        })
    }
}