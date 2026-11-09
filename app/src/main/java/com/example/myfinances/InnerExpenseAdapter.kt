package com.example.myfinances

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

class InnerExpenseAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<InnerExpenseAdapter.ExpenseViewHolder>() {

    fun updateData(newExpenses: List<Expense>) {
        this.expenses = newExpenses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inner_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvExpenseName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvExpenseDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
        private val viewCategoryColor: View = itemView.findViewById(R.id.viewCategoryColor)

        fun bind(expense: Expense) {
            tvName.text = expense.name
            tvDate.text = expense.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            tvAmount.text = "${expense.amount.toInt()} zł"

            // Ustawienie koloru kropki kategorii
            val bgShape = viewCategoryColor.background as? GradientDrawable
            bgShape?.setColor(expense.category.color.toArgb())
        }
    }
}