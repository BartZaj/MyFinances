package com.example.myfinances

import android.content.Intent
import android.os.Trace
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    expensesList: List<Expense>,
    isLoading: Boolean
) {
    DisposableEffect(Unit) {
        Trace.beginSection("ComposeScreen_ExpensesChart")
        onDispose {
            Trace.endSection()
        }
    }

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    // Stan limitów z Firebase oraz aktualnie edytowanej kategorii
    var categoryLimits by remember { mutableStateOf(emptyMap<ExpenseCategory, Float>()) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    // Nasłuchiwanie gałęzi Users -> {uid} -> Limits
    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            return@DisposableEffect onDispose {}
        }

        val limitsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Limits")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetched = mutableMapOf<ExpenseCategory, Float>()
                for (child in snapshot.children) {
                    val catName = child.key ?: continue
                    val limitVal = child.getValue(Double::class.java)?.toFloat() ?: 0f
                    try {
                        fetched[ExpenseCategory.valueOf(catName)] = limitVal
                    } catch (_: IllegalArgumentException) {
                    }
                }
                categoryLimits = fetched
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        limitsRef.addValueEventListener(listener)
        onDispose { limitsRef.removeEventListener(listener) }
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Dodaj") },
                text = { Text("Dodaj wydatek") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF74C69D))
                }
            } else {
                // Filtrowanie dla obecnego miesiąca (tak jak w XML)
                val currentMonthExpenses = remember(expensesList) {
                    val now = LocalDate.now()
                    expensesList.filter {
                        it.date.monthValue == now.monthValue && it.date.year == now.year
                    }
                }

                val categoryTotals = remember(currentMonthExpenses) {
                    currentMonthExpenses.groupBy { it.category }
                        .mapValues { entry ->
                            entry.value.sumOf { it.amount.toDouble() }.toFloat()
                        }
                }

                val maxTotal = categoryTotals.values.maxOrNull() ?: 0f
                val yAxisMax = when {
                    maxTotal >= 1000f -> ((maxTotal / 500).toInt() + 1) * 500f
                    maxTotal > 0f -> ((maxTotal / 100).toInt() + 1) * 100f
                    else -> 100f
                }

                val stepCount = 4
                val stepValue = yAxisMax / stepCount
                val categories = remember { ExpenseCategory.values() }
                val pagerState = rememberPagerState(pageCount = { categories.size })

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Podsumowanie Wydatków",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
                    )

                    val xAxisLabelSpace = 40.dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(bottom = xAxisLabelSpace, end = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in stepCount downTo 0) {
                                Text(
                                    text = "${(stepValue * i).toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.offset(y = (-8).dp)
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(bottom = xAxisLabelSpace),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (i in stepCount downTo 0) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                    ) {
                                        drawLine(
                                            color = Color.LightGray,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                categories.forEach { category ->
                                    val totalForCategory = categoryTotals[category] ?: 0f
                                    val heightPercentage = if (yAxisMax > 0) totalForCategory / yAxisMax else 0f

                                    BarChartItem(
                                        category = category,
                                        total = totalForCategory,
                                        heightPercentage = heightPercentage,
                                        xAxisLabelSpace = xAxisLabelSpace,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                val intent = Intent(context, CategoryDetailsActivity::class.java).apply {
                                                    putExtra("CATEGORY_NAME", category.name)
                                                }
                                                context.startActivity(intent)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // MIEJSCE NA 3 OSTATNIE TRANSAKCJE (W PRZYSZŁOŚCI)
                    // ==========================================
                    Spacer(modifier = Modifier.height(24.dp))

                    // Sekcja limitów kategorii
                    Text(
                        text = "Limity kategorii (przesuń w bok)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) { page ->
                        val category = categories[page]
                        CategoryLimitCard(
                            category = category,
                            spent = categoryTotals[category] ?: 0f,
                            limit = categoryLimits[category] ?: 0f,
                            onEditClick = { editingCategory = category }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kropki paginacji
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEachIndexed { index, cat ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) cat.color else Color.LightGray)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, amount, date, category ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")
                    val newExpenseRef = dbRef.push()
                    val expenseData = mapOf(
                        "name" to name,
                        "amount" to amount,
                        "date" to date.toString(),
                        "category" to category.name
                    )
                    newExpenseRef.setValue(expenseData)
                }
                showAddDialog = false
            }
        )
    }

    // Wyświetlanie BottomSheet z edycją limitu (arkusz idzie w górę razem z klawiaturą)
    editingCategory?.let { category ->
        val currentLimit = categoryLimits[category] ?: 0f
        val saveLimit: (Float) -> Unit = { newLimit ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .child("Limits")
                    .child(category.name)
                    .setValue(newLimit.toDouble())
            }
            editingCategory = null
        }

        EditLimitBottomSheetWithKeyboard(
            category = category,
            currentLimit = currentLimit,
            onDismiss = { editingCategory = null },
            onSave = saveLimit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Float, date: LocalDate, category: ExpenseCategory) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val interactionSource = remember { MutableInteractionSource() }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj wydatek") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa wydatku") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Kwota") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = selectedDate.toString(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExpenseCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = amountText.toFloatOrNull() ?: 0f
                    if (name.isNotBlank() && finalAmount > 0) {
                        onAdd(name, finalAmount, selectedDate, selectedCategory)
                    }
                }
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Wyjdź")
            }
        }
    )
}

@Composable
fun BarChartItem(
    category: ExpenseCategory,
    total: Float,
    heightPercentage: Float,
    xAxisLabelSpace: Dp,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val animatedFraction by animateFloatAsState(
        targetValue = if (startAnimation) heightPercentage else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            delayMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "BarHeightAnimation"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Spacer(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val barHeightPx = size.height * animatedFraction.coerceAtLeast(0.01f)
                        val topY = size.height - barHeightPx
                        val cornerPx = 6.dp.toPx()

                        drawRoundRect(
                            color = category.color,
                            topLeft = Offset(0f, topY),
                            size = Size(size.width, barHeightPx),
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )

                        if (barHeightPx > cornerPx) {
                            drawRect(
                                color = category.color,
                                topLeft = Offset(0f, topY + barHeightPx - cornerPx),
                                size = Size(size.width, cornerPx)
                            )
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val barHeightPx = size.height * animatedFraction.coerceAtLeast(0.01f)
                        translationY = -barHeightPx - 4.dp.toPx()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (total > 0) {
                    Text(
                        text = "${total.toInt()} zł",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .height(xAxisLabelSpace)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                imageVector = limitCategoryIcon(category),
                contentDescription = category.displayName,
                tint = category.color, // Ikona w kolorze kategorii
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(24.dp)
            )
        }
    }
}

// Ikony kategorii (te same, co w pozostałych ekranach)
private fun limitCategoryIcon(category: ExpenseCategory): ImageVector =
    when (category.name) {
        "FOOD" -> Icons.Default.ShoppingCart
        "TRANSPORT" -> Icons.Default.DirectionsCar
        "ENTERTAINMENT" -> Icons.Default.MusicNote
        "BILLS" -> Icons.Default.ReceiptLong
        "CLOTHING" -> Icons.Default.Checkroom
        "HEALTH" -> Icons.Default.MedicalServices
        else -> Icons.Default.List
    }

// Karta limitu: jasne tło, wykres kołowy z procentem, przycisk edycji w prawym dolnym rogu
@Composable
fun CategoryLimitCard(
    category: ExpenseCategory,
    spent: Float,
    limit: Float,
    onEditClick: () -> Unit
) {
    val hasLimit = limit > 0f
    val isExceeded = hasLimit && spent > limit
    val rawPercent = if (hasLimit) ((spent / limit) * 100).toInt() else 0
    val targetProgress = if (hasLimit) (spent / limit).coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "LimitProgressAnimation"
    )

    val errorColor = Color(0xFFD32F2F)
    val indicatorColor = if (isExceeded) errorColor else category.color
    // Kolor tła karty – podmień na kolor karty „ukończone” z celów oszczędnościowych
    val cardColor = Color(0xFFF2F4F3)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(96.dp),
                        color = indicatorColor,
                        strokeWidth = 10.dp,
                        trackColor = Color(0xFFB0B0B0),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = if (hasLimit) "$rawPercent%" else "—",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Kolorowe kółko z białą ikoną + nazwa kategorii
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(category.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = limitCategoryIcon(category),
                                contentDescription = category.displayName,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = category.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = if (hasLimit) {
                            "Bilans: ${spent.toInt()} zł / ${limit.toInt()} zł"
                        } else {
                            "Bilans: ${spent.toInt()} zł / Brak limitu"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                }
            }

            FilledTonalButton(
                onClick = onEditClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = category.color,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .height(36.dp)
            ) {
                Text("Edytuj limit", fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLimitBottomSheetWithKeyboard(
    category: ExpenseCategory,
    currentLimit: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var limitText by remember(category, currentLimit) {
        mutableStateOf(if (currentLimit > 0f) currentLimit.toInt().toString() else "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Limit: ${category.displayName}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = limitText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        limitText = newValue
                    }
                },
                label = { Text("Miesięczny limit (zł)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Anuluj")
                }
                Button(
                    onClick = {
                        val parsed = limitText.toFloatOrNull()
                        if (parsed != null && parsed >= 0f) {
                            onSave(parsed)
                        } else {
                            Toast.makeText(context, "Wprowadź poprawną kwotę", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Zapisz")
                }
            }
        }
    }
}