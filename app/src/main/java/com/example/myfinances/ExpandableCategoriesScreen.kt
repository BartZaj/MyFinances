package com.example.myfinances

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth

@Composable
fun ExpandableCategoriesScreen(
    selectedMonthStr: String?,
    expensesList: List<Expense>,
    isLoading: Boolean,
    onExpenseClick: (String) -> Unit
) {
    var expandedCategoryNames by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val targetMonth = remember(selectedMonthStr) {
        selectedMonthStr?.let { YearMonth.parse(it) }
    }

    val groupedExpenses = remember(expensesList, targetMonth) {
        expensesList
            .filter { targetMonth == null || YearMonth.from(it.date) == targetMonth }
            .groupBy { it.category }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val sortedCategories = remember(groupedExpenses) {
            val order = listOf("FOOD", "TRANSPORT", "ENTERTAINMENT", "BILLS", "CLOTHING", "HEALTH", "OTHER")
            groupedExpenses.keys.sortedBy { category ->
                val index = order.indexOf(category.name)
                if (index == -1) Int.MAX_VALUE else index
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = sortedCategories,
                key = { category -> category.name }
            ) { category ->
                val expenses = groupedExpenses[category]?.sortedWith(
                    compareByDescending<Expense> { it.date }
                        .thenByDescending { it.id }
                ) ?: emptyList()
                val isExpanded = expandedCategoryNames.contains(category.name)

                ExpandableCategoryCard(
                    category = category,
                    expenses = expenses,
                    isExpanded = isExpanded,
                    onExpandChange = { expanded ->
                        expandedCategoryNames = if (expanded) {
                            expandedCategoryNames + category.name
                        } else {
                            expandedCategoryNames - category.name
                        }
                    },
                    onExpenseClick = onExpenseClick
                )
            }
        }
    }
}

@Composable
fun ExpandableCategoryCard(
    category: ExpenseCategory,
    expenses: List<Expense>,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onExpenseClick: (String) -> Unit
) {
    val totalSum = remember(expenses) {
        expenses.sumOf { it.amount.toDouble() }.toFloat()
    }

    val interactionSource = remember { MutableInteractionSource() }
    val baseColor = Color(category.color.toArgb())
    val backgroundColor = baseColor.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onExpandChange(!isExpanded)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val categoryIcon = when (category.name) {
                    "FOOD" -> Icons.Default.ShoppingCart
                    "TRANSPORT" -> Icons.Default.DirectionsCar
                    "ENTERTAINMENT" -> Icons.Default.MusicNote
                    "CLOTHING" -> Icons.Default.Checkroom
                    "HEALTH" -> Icons.Default.MedicalServices
                    "BILLS" -> Icons.Default.ReceiptLong
                    else -> Icons.Default.List
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(baseColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category.displayName,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = category.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = baseColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.2f zł", totalSum),
                    fontWeight = FontWeight.Bold,
                    color = baseColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Rozwiń/Zwiń",
                    tint = baseColor
                )
            }
        }

        if (isExpanded) {
            Divider(color = baseColor.copy(alpha = 0.4f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                expenses.forEach { expense ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpenseClick(expense.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(baseColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = expense.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = expense.date.toString(),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = String.format("- %.2f zł", expense.amount),
                            color = baseColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}