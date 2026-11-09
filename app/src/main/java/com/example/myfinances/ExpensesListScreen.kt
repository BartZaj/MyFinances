package com.example.myfinances

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    // Filtry kwot "Od - Do" (używamy String, aby poprawnie obsługiwać wpisywanie z klawiatury)
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    // 2. OPTYMALIZACJA PAMIĘCIOWA (Filtrowanie)
    // Bezpieczne parsowanie kwot
    val minAmount = minAmountText.toFloatOrNull()
    val maxAmount = maxAmountText.toFloatOrNull()

    // Ten blok wykona się tylko przy wpisywaniu/zmianie filtrów lub zmianie bazy,
    // chroniąc pamięć przed alokacjami w trakcie scrollowania!
    val filteredList = remember(
        expensesList, selectedCategory, startDate, endDate, minAmount, maxAmount
    ) {
        expensesList.filter { expense ->
            val categoryMatch = selectedCategory == null || expense.category == selectedCategory

            // isBefore i isAfter upewniają się, że krawędzie (ta sama data) też są wliczane
            val startDateMatch = startDate == null || !expense.date.isBefore(startDate)
            val endDateMatch = endDate == null || !expense.date.isAfter(endDate)

            val minAmountMatch = minAmount == null || expense.amount >= minAmount
            val maxAmountMatch = maxAmount == null || expense.amount <= maxAmount

            categoryMatch && startDateMatch && endDateMatch && minAmountMatch && maxAmountMatch
        }.sortedByDescending { it.date }
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
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Wszystkie") }
                        )
                    }
                    items(ExpenseCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = category.color.copy(alpha = 0.2f),
                                selectedLabelColor = Color.Black
                            )
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
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it },
                        label = { Text("Do kwoty (zł)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
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
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = startDate?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "Od daty",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (startDate != null) {
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
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = endDate?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "Do daty",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (endDate != null) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.id } // Kluczowe dla wydajności przy scrollowaniu i zmianach filtrowania!
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

// (Funkcja ExpenseListItemCard pozostaje bez zmian jak w poprzedniej wiadomości)
@Composable
fun ExpenseListItemCard(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(expense.category.color, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = "${expense.amount.toInt()} zł",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}