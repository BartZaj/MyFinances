package com.example.myfinances

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Float = 0f,
    val currentAmount: Float = 0f,
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val isCompleted: Boolean = false
) {
    val progressPercent: Int
        get() = if (targetAmount > 0f) {
            ((currentAmount / targetAmount) * 100f).toInt()
        } else 0

    val isGoalReached: Boolean
        get() = targetAmount > 0f && currentAmount >= targetAmount
}