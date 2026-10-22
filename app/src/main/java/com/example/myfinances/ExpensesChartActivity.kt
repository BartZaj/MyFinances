package com.example.myfinances

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myfinances.ui.theme.MyFinancesTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExpensesChartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFinancesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpensesScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    val context = LocalContext.current

    var expensesList by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Flaga do wyświetlania okna dodawania wydatku
    var showAddDialog by remember { mutableStateOf(false) }

    // Używamy DisposableEffect z addValueEventListener,
    // aby nasłuchiwać zmian w bazie NA ŻYWO.
    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // Baza pusta: inicjujemy przykładowymi danymi
                    mockExpenses.forEach { expense ->
                        val newExpenseRef = databaseRef.push()
                        val expenseData = mapOf(
                            "name" to expense.name,
                            "amount" to expense.amount,
                            "date" to expense.date.toString(),
                            "category" to expense.category.name
                        )
                        newExpenseRef.setValue(expenseData)
                    }
                    expensesList = mockExpenses
                    isLoading = false
                } else {
                    val fetchedExpenses = mutableListOf<Expense>()
                    for (child in snapshot.children) {
                        try {
                            val name = child.child("name").getValue(String::class.java) ?: ""
                            val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val dateStr = child.child("date").getValue(String::class.java) ?: ""
                            val categoryStr = child.child("category").getValue(String::class.java) ?: ""

                            val date = LocalDate.parse(dateStr)
                            val category = ExpenseCategory.valueOf(categoryStr)

                            fetchedExpenses.add(Expense(name, amount, date, category))
                        } catch (e: Exception) {
                            Log.e("Firebase", "Błąd parsowania wydatku: ${e.message}")
                        }
                    }
                    expensesList = fetchedExpenses
                    isLoading = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd bazy danych: ${error.message}")
                isLoading = false
            }
        }

        databaseRef.addValueEventListener(listener)

        // Odpięcie nasłuchiwacza przy niszczeniu widoku
        onDispose {
            databaseRef.removeEventListener(listener)
        }
    }

    Scaffold(
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
                val categoryTotals = remember(expensesList) {
                    expensesList.groupBy { it.category }
                        .mapValues { entry ->
                            entry.value.sumOf { it.amount.toDouble() }.toFloat()
                        }
                }

                val maxTotal = categoryTotals.values.maxOrNull() ?: 0f
                val yAxisMax = if (maxTotal > 0f) {
                    ((maxTotal / 100).toInt() + 1) * 100f
                } else 100f

                val stepCount = 4
                val stepValue = yAxisMax / stepCount

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Podsumowanie Wydatków",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp, top = 16.dp)
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
                                ExpenseCategory.values().forEach { category ->
                                    val totalForCategory = categoryTotals[category] ?: 0f
                                    val heightPercentage = if (yAxisMax > 0) totalForCategory / yAxisMax else 0f

                                    BarChartItem(
                                        category = category,
                                        total = totalForCategory,
                                        heightPercentage = heightPercentage,
                                        xAxisLabelSpace = xAxisLabelSpace,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
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

    // Stan kalendarza (DatePicker)
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
                // Pole nazwy
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa wydatku") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Pole kwoty z weryfikacją do max 2 miejsc po przecinku
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

                // Pole daty (wywołujące kalendarz)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = selectedDate.toString(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false, // Wyłącza domyślne zachowanie wpisywania na rzecz kliknięcia w Box
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Dropdown dla kategorii
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
                    .width(44.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        // POPRAWKA: Dodano znak mnożenia (*) oraz odejmowania (-)
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
                        // POPRAWKA: Dodano znak mnożenia (*)
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
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}