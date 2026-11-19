package com.example.myfinances

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

data class CategoryLimitItem(
    val category: ExpenseCategory,
    val spent: Float,
    val limit: Float
)

class CategoryLimitAdapter(
    private val currentPageProvider: () -> Int,
    private val onEditClick: (ExpenseCategory, Float) -> Unit
) : RecyclerView.Adapter<CategoryLimitAdapter.LimitViewHolder>() {

    private var items: List<CategoryLimitItem> = emptyList()
    private var hasPlayedInitialAnimation = false

    fun submitList(newItems: List<CategoryLimitItem>) {
        if (items.size != newItems.size) {
            items = newItems
            notifyDataSetChanged()
            return
        }

        val oldItems = items
        items = newItems
        for (i in newItems.indices) {
            if (oldItems[i] != newItems[i]) {
                notifyItemChanged(i, "UPDATE")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LimitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_limit_card, parent, false)
        return LimitViewHolder(view)
    }

    override fun onBindViewHolder(holder: LimitViewHolder, position: Int) {
        val shouldAnimate = !hasPlayedInitialAnimation && position == currentPageProvider()
        if (shouldAnimate) {
            hasPlayedInitialAnimation = true
        }
        holder.bind(items[position], animate = shouldAnimate)
    }

    override fun onBindViewHolder(
        holder: LimitViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains("UPDATE")) {
            // Animuj aktualizację danych tylko na aktualnie widocznej karcie
            val isVisiblePage = (position == currentPageProvider())
            holder.bind(items[position], animate = isVisiblePage)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class LimitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressLimit: LimitCircularChartView = itemView.findViewById(R.id.progressLimit)
        private val tvLimitPercent: TextView = itemView.findViewById(R.id.tvLimitPercent)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvLimitStatus: TextView = itemView.findViewById(R.id.tvLimitStatus)
        private val btnEditLimit: MaterialButton = itemView.findViewById(R.id.btnEditLimit)

        fun bind(item: CategoryLimitItem, animate: Boolean) {
            tvCategoryName.text = item.category.displayName

            if (item.limit > 0f) {
                val rawPercent = ((item.spent / item.limit) * 100).toInt()
                val progressFraction = (item.spent / item.limit).coerceIn(0f, 1f)

                tvLimitPercent.text = "$rawPercent%"
                tvLimitStatus.text = "Wydano: ${item.spent.toInt()} zł / ${item.limit.toInt()} zł"

                val indicatorColor = if (rawPercent > 100) {
                    Color.parseColor("#D32F2F")
                } else {
                    item.category.color.toArgb()
                }
                progressLimit.setProgressData(item.category.name, progressFraction, indicatorColor, animate)
            } else {
                tvLimitPercent.text = "—"
                tvLimitStatus.text = "Wydano: ${item.spent.toInt()} zł / Brak limitu"
                progressLimit.setProgressData(item.category.name, 0f, item.category.color.toArgb(), animate)
            }

            btnEditLimit.setOnClickListener {
                onEditClick(item.category, item.limit)
            }
        }
    }
}