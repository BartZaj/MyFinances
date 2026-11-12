package com.example.myfinances

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

// Te same ikony co w wersji Compose
fun categoryIconRes(category: ExpenseCategory): Int = when (category.name) {
    "FOOD" -> R.drawable.baseline_directions_car_24
    "TRANSPORT" -> R.drawable.baseline_directions_car_24
    "ENTERTAINMENT" -> R.drawable.baseline_directions_car_24
    "CLOTHING" -> R.drawable.baseline_directions_car_24
    "HEALTH" -> R.drawable.baseline_directions_car_24
    "BILLS" -> R.drawable.baseline_directions_car_24
    else -> R.drawable.baseline_directions_car_24
}

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
        private val flCategoryIcon: FrameLayout = itemView.findViewById(R.id.flCategoryIcon)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(expense: Expense) {
            val baseColor = expense.category.color.toArgb()

            tvName.text = expense.name
            tvName.setTextColor(baseColor)

            tvDate.text = expense.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

            tvAmount.text = String.format("- %.2f zł", expense.amount)
            tvAmount.setTextColor(baseColor)

            ivCategoryIcon.setImageResource(categoryIconRes(expense.category))

            // mutate(), żeby nie farbować współdzielonego drawable'a wszystkich elementów naraz
            (itemView.background.mutate() as? GradientDrawable)
                ?.setColor(ColorUtils.setAlphaComponent(baseColor, 46)) // 0.18 * 255
            (flCategoryIcon.background.mutate() as? GradientDrawable)
                ?.setColor(baseColor)
        }
    }
}