package com.example.myfinances

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class SavingsGoalAdapter(
    private val onAddFundsClick: (SavingsGoal) -> Unit,
    private val onAddAsExpenseClick: (SavingsGoal) -> Unit,
    private val onDeleteClick: (SavingsGoal) -> Unit
) : ListAdapter<SavingsGoal, SavingsGoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    private val lastProgressMap = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flCategoryIcon: FrameLayout = itemView.findViewById(R.id.flCategoryIcon)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        private val tvGoalCategory: TextView = itemView.findViewById(R.id.tvGoalCategory)
        private val tvCompletedBadge: TextView = itemView.findViewById(R.id.tvCompletedBadge)
        private val btnDeleteGoal: ImageButton = itemView.findViewById(R.id.btnDeleteGoal)
        private val tvGoalAmounts: TextView = itemView.findViewById(R.id.tvGoalAmounts)
        private val tvGoalPercent: TextView = itemView.findViewById(R.id.tvGoalPercent)
        private val progressGoal: LinearProgressIndicator = itemView.findViewById(R.id.progressGoal)
        private val llActions: LinearLayout = itemView.findViewById(R.id.llActions)
        private val btnAddFunds: MaterialButton = itemView.findViewById(R.id.btnAddFunds)
        private val btnAddAsExpense: MaterialButton = itemView.findViewById(R.id.btnAddAsExpense)

        fun bind(goal: SavingsGoal) {
            val baseColor = goal.category.color.toArgb()

            // Tło karty i kółko ikony (identycznie jak w InnerExpenseAdapter)
            (itemView.background.mutate() as? GradientDrawable)?.setColor(
                if (goal.isCompleted) Color.parseColor("#F2F4F3")
                else ColorUtils.setAlphaComponent(baseColor, 46)
            )
            (flCategoryIcon.background.mutate() as? GradientDrawable)?.setColor(
                if (goal.isCompleted) ColorUtils.setAlphaComponent(baseColor, 140)
                else baseColor
            )
            ivCategoryIcon.setImageResource(categoryIconRes(goal.category))

            tvGoalName.text = goal.name
            tvGoalName.setTextColor(Color.BLACK)

            tvGoalCategory.text = goal.category.displayName

            tvGoalAmounts.text = String.format("%.2f / %.2f zł", goal.currentAmount, goal.targetAmount)
            tvGoalAmounts.setTextColor(Color.BLACK)


            tvGoalPercent.text = "${goal.progressPercent}%"
            tvGoalPercent.setTextColor(if (goal.isCompleted) Color.parseColor("#2D6A4F") else baseColor)

            // Kolory paska progresu
            val barColor = if (goal.isCompleted) Color.parseColor("#52B788") else baseColor
            progressGoal.setIndicatorColor(barColor)
            progressGoal.trackColor = ColorUtils.setAlphaComponent(barColor, 60)

            // Animacja paska progresu
            (progressGoal.tag as? ObjectAnimator)?.cancel()
            val targetProgress = if (goal.targetAmount > 0f) {
                ((goal.currentAmount / goal.targetAmount).coerceIn(0f, 1f) * 1000f).toInt()
            } else 0

            val previousProgress = lastProgressMap[goal.id]
            if (previousProgress != null && previousProgress != targetProgress) {
                val animator = ObjectAnimator.ofInt(progressGoal, "progress", previousProgress, targetProgress).apply {
                    duration = 500L
                    interpolator = DecelerateInterpolator()
                    start()
                }
                progressGoal.tag = animator
            } else {
                progressGoal.progress = targetProgress
            }
            lastProgressMap[goal.id] = targetProgress

            // Stan ukończony vs aktywny
            if (goal.isCompleted) {
                tvCompletedBadge.visibility = View.VISIBLE
                (tvCompletedBadge.background.mutate() as? GradientDrawable)
                    ?.setColor(Color.parseColor("#D8F3DC"))
                llActions.visibility = View.GONE
            } else {
                tvCompletedBadge.visibility = View.GONE
                llActions.visibility = View.VISIBLE

                btnAddFunds.backgroundTintList = ColorStateList.valueOf(baseColor)
                btnAddFunds.setTextColor(Color.WHITE)
                btnAddFunds.setOnClickListener { onAddFundsClick(goal) }

                if (goal.isGoalReached) {
                    btnAddAsExpense.visibility = View.VISIBLE
                    btnAddAsExpense.setOnClickListener { onAddAsExpenseClick(goal) }
                } else {
                    btnAddAsExpense.visibility = View.GONE
                }
            }

            // Przycisk usuwania zawsze aktywny
            btnDeleteGoal.setOnClickListener { onDeleteClick(goal) }
        }
    }

    private class GoalDiffCallback : DiffUtil.ItemCallback<SavingsGoal>() {
        override fun areItemsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean =
            oldItem == newItem
    }
}