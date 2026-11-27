package com.example.myfinances

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    goalsList: List<SavingsGoal>,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalForFundsSheet by remember { mutableStateOf<SavingsGoal?>(null) }
    var shouldScrollToTop by remember { mutableStateOf(false) }

    val sortedGoals = remember(goalsList) {
        goalsList.sortedWith(
            compareBy<SavingsGoal> { it.isCompleted }
                .thenByDescending { it.id }
        )
    }

    // Płynne przewinięcie na górę listy zaraz po dodaniu nowego celu
    LaunchedEffect(sortedGoals) {
        if (shouldScrollToTop && sortedGoals.isNotEmpty()) {
            listState.animateScrollToItem(0)
            shouldScrollToTop = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Górny pasek z tytułem i przyciskiem dodawania
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cele oszczędnościowe",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { showAddGoalDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD1E8E2),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Dodaj cel")
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0x4DCCCCCC))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF74C69D))
                }
            }

            sortedGoals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak celów oszczędnościowych.\nDodaj pierwszy cel przyciskiem powyżej.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = sortedGoals,
                        key = { "${it.id}_${it.isCompleted}" }
                    ) { goal ->
                        SavingsGoalCard(
                            goal = goal,
                            modifier = Modifier.animateItem(),
                            onAddFundsClick = { goalForFundsSheet = goal },
                            onAddAsExpenseClick = {
                                convertGoalToExpense(goal) {
                                    Toast.makeText(context, "Dodano jako wydatek", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteClick = { deleteGoal(goal) }
                        )
                    }
                }
            }
        }
    }

    // Dialog dodawania nowego celu
    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, targetAmount, category ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    shouldScrollToTop = true
                    val newGoalRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(uid)
                        .child("SavingsGoals")
                        .push()

                    val goalData = mapOf(
                        "name" to name,
                        "targetAmount" to targetAmount.toDouble(),
                        "currentAmount" to 0.0,
                        "category" to category.name,
                        "completed" to false
                    )
                    newGoalRef.setValue(goalData)
                }
                showAddGoalDialog = false
            }
        )
    }

    // BottomSheet do dopłacania kwoty do celu
    goalForFundsSheet?.let { selectedGoal ->
        AddFundsBottomSheet(
            goal = selectedGoal,
            onDismiss = { goalForFundsSheet = null },
            onConfirm = { addedAmount ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val newTotal = (selectedGoal.currentAmount + addedAmount).toDouble()
                    FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(uid)
                        .child("SavingsGoals")
                        .child(selectedGoal.id)
                        .child("currentAmount")
                        .setValue(newTotal)
                }
                goalForFundsSheet = null
            }
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    onAddFundsClick: () -> Unit,
    onAddAsExpenseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = goal.category.color
    val cardBackground = if (goal.isCompleted) {
        Color(0xFFF2F4F3)
    } else {
        baseColor.copy(alpha = 0.18f)
    }
    val iconCircleColor = if (goal.isCompleted) {
        baseColor.copy(alpha = 0.55f)
    } else {
        baseColor
    }
    val barColor = if (goal.isCompleted) Color(0xFF52B788) else baseColor

    val targetProgress = if (goal.targetAmount > 0f) {
        (goal.currentAmount / goal.targetAmount).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "goalProgressAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .padding(16.dp)
    ) {
        // Górny wiersz: ikona kategorii + nazwa + status/usuń
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconCircleColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = categoryIconRes(goal.category)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = goal.category.displayName,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (goal.isCompleted) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD8F3DC))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ukończono",
                        color = Color(0xFF2D6A4F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń cel",
                    tint = Color(0xFF666666)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kwoty i procent
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.2f / %.2f zł", goal.currentAmount, goal.targetAmount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${goal.progressPercent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Animowany pasek progresu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(barColor.copy(alpha = 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor)
            )
        }

        // Dolne przyciski akcji (ukryte po ukończeniu)
        if (!goal.isCompleted) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (goal.isGoalReached) {
                    FilledTonalButton(
                        onClick = onAddAsExpenseClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = "Dodaj jako wydatek", fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = onAddFundsClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = baseColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Dodaj", fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmount: Float, category: ExpenseCategory) -> Unit
) {
    val context = LocalContext.current
    val categories = ExpenseCategory.values()

    var name by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: ExpenseCategory.FOOD) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj cel oszczędnościowy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa celu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            targetAmountText = input
                        }
                    },
                    label = { Text("Kwota docelowa (zł)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategoria") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    selectedCategory = category
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val amount = targetAmountText.toFloatOrNull() ?: 0f
                    if (trimmedName.isNotBlank() && amount > 0f) {
                        onConfirm(trimmedName, amount, selectedCategory)
                    } else {
                        Toast.makeText(context, "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFundsBottomSheet(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Dodaj do: ${goal.name}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text("Kwota do dodania (zł)") },
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
                        val addedAmount = amountText.toFloatOrNull()
                        if (addedAmount != null && addedAmount > 0f) {
                            onConfirm(addedAmount)
                        } else {
                            Toast.makeText(context, "Wprowadź poprawną kwotę", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Dodaj")
                }
            }
        }
    }
}

private fun convertGoalToExpense(goal: SavingsGoal, onDone: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

    val expenseData = mapOf(
        "name" to goal.name,
        "amount" to goal.currentAmount,
        "date" to LocalDate.now().toString(),
        "category" to goal.category.name
    )
    userRef.child("Expenses").push().setValue(expenseData)

    userRef.child("SavingsGoals")
        .child(goal.id)
        .child("completed")
        .setValue(true)

    onDone()
}

private fun deleteGoal(goal: SavingsGoal) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    FirebaseDatabase.getInstance()
        .getReference("Users")
        .child(uid)
        .child("SavingsGoals")
        .child(goal.id)
        .removeValue()
}