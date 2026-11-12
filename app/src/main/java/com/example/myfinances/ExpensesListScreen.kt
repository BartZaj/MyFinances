package com.example.myfinances

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Ikony kategorii (te same, co w ExpandableCategoriesActivity)
private fun categoryIconFor(category: ExpenseCategory): ImageVector =
    when (category.name) {
        "FOOD" -> Icons.Default.ShoppingCart
        "TRANSPORT" -> Icons.Default.DirectionsCar
        "ENTERTAINMENT" -> Icons.Default.MusicNote
        "CLOTHING" -> Icons.Default.Checkroom
        "HEALTH" -> Icons.Default.MedicalServices
        "BILLS" -> Icons.Default.ReceiptLong
        else -> Icons.Default.List
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    expensesList: List<Expense>,
    isLoading: Boolean
) {
    // 1. STANY FILTRÓW
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    // Filtry dat "Od - Do"
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Filtry kwot "Od - Do"
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    // 2. OPTYMALIZACJA PAMIĘCIOWA (Filtrowanie)
    val minAmount = minAmountText.toFloatOrNull()
    val maxAmount = maxAmountText.toFloatOrNull()

    val filteredList = remember(
        expensesList, selectedCategory, startDate, endDate, minAmount, maxAmount
    ) {
        expensesList.filter { expense ->
            val categoryMatch = selectedCategory == null || expense.category == selectedCategory

            val startDateMatch = startDate == null || !expense.date.isBefore(startDate)
            val endDateMatch = endDate == null || !expense.date.isAfter(endDate)

            val minAmountMatch = minAmount == null || expense.amount >= minAmount
            val maxAmountMatch = maxAmount == null || expense.amount <= maxAmount

            categoryMatch && startDateMatch && endDateMatch && minAmountMatch && maxAmountMatch
        }.sortedByDescending { it.date }
    }

    // Czysty stan listy (od pozycji 0) ZAWSZE, gdy zmieni się którykolwiek filtr.
    val listState = remember(selectedCategory, startDate, endDate, minAmountText, maxAmountText) {
        LazyListState()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // ==========================================
            // SEKCJA FILTRÓW
            // ==========================================
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Wszystkie wydatki",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // A. KATEGORIE
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    item {
                        CategoryFilterChip(
                            label = "Wszystkie",
                            icon = Icons.Default.List,
                            baseColor = Color.Gray,
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(ExpenseCategory.values()) { category ->
                        CategoryFilterChip(
                            label = category.displayName,
                            icon = categoryIconFor(category),
                            baseColor = Color(category.color.toArgb()),
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }

                // B. KWOTY (Od - Do)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { minAmountText = it },
                        label = { Text("Od kwoty (zł)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it },
                        label = { Text("Do kwoty (zł)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // C. DATY (Od - Do)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Przycisk daty OD
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = startDate?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "Od daty",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (startDate != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Usuń",
                                modifier = Modifier.size(16.dp).clickable { startDate = null }
                            )
                        }
                    }

                    // Przycisk daty DO
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = endDate?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "Do daty",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (endDate != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Usuń",
                                modifier = Modifier.size(16.dp).clickable { endDate = null }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            // ==========================================
            // SEKCJA LISTY WYDATKÓW
            // ==========================================
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF74C69D))
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak wydatków spełniających kryteria.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.id }
                    ) { expense ->
                        ExpenseListItemCard(expense = expense)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGI DAT
    // ==========================================
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Anuluj") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Anuluj") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Chip kategorii w stylu ekranu kategorii (kółko z ikoną + kolorowe tło po zaznaczeniu)
@Composable
private fun CategoryFilterChip(
    label: String,
    icon: ImageVector,
    baseColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color.LightGray,
                shape = shape
            )
            .background(
                color = if (isSelected) baseColor.copy(alpha = 0.18f) else Color.Transparent,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Brak efektu fali
                onClick = onClick
            )
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) baseColor else baseColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isSelected) baseColor else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// Karta wydatku w stylu ExpandableCategoryCard
@Composable
fun ExpenseListItemCard(expense: Expense) {
    val baseColor = Color(expense.category.color.toArgb())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(baseColor.copy(alpha = 0.18f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolorowe kółko z białą ikoną
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(baseColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIconFor(expense.category),
                    contentDescription = expense.category.displayName,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = expense.name,
                    color = baseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = expense.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = String.format("- %.2f zł", expense.amount),
            color = baseColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}