package com.example.myfinances

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chart : Screen("chart", "Wykres", Icons.Filled.PieChart)
    object Categories : Screen("categories", "Kategorie", Icons.Filled.List)
    object ExpensesList : Screen("expenses_list", "Lista", Icons.Filled.FormatListBulleted)
    object SavingsGoals : Screen("savings_goals", "Cele", Icons.Filled.Flag)
}

object DetailRoutes {
    const val CATEGORY_DETAILS = "category_details/{categoryName}"
    const val EXPANDABLE_CATEGORIES = "expandable_categories?selectedMonth={selectedMonth}"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"

    fun categoryDetails(categoryName: String) = "category_details/$categoryName"
    fun expandableCategories(selectedMonth: String?) =
        if (selectedMonth != null) "expandable_categories?selectedMonth=$selectedMonth"
        else "expandable_categories"
    fun expenseDetail(expenseId: String) = "expense_detail/$expenseId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarItems = listOf(
        Screen.Chart,
        Screen.Categories,
        Screen.ExpensesList,
        Screen.SavingsGoals
    )
    val detailRoutes = remember {
        setOf(
            DetailRoutes.CATEGORY_DETAILS,
            DetailRoutes.EXPANDABLE_CATEGORIES,
            DetailRoutes.EXPENSE_DETAIL
        )
    }
    val showBottomBar = bottomBarItems.any { it.route == currentRoute }

    // Stany dla wydatków
    var expensesList by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Stany dla celów oszczędnościowych
    var goalsList by remember { mutableStateOf<List<SavingsGoal>>(emptyList()) }
    var isGoalsLoading by remember { mutableStateOf(true) }

    // Zapamiętane wysokości słupków przed przejściem do szczegółów kategorii
    var savedChartFractions by remember { mutableStateOf<Map<ExpenseCategory, Float>?>(null) }

    // Pobieranie wydatków i celów z Firebase raz dla całej sesji
    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isLoading = false
            isGoalsLoading = false
            return@DisposableEffect onDispose {}
        }

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        val expensesRef = userRef.child("Expenses")
        val goalsRef = userRef.child("SavingsGoals")

        val expensesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    expensesList = emptyList()
                    isLoading = false
                } else {
                    val fetchedExpenses = mutableListOf<Expense>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.key ?: ""
                            val name = child.child("name").getValue(String::class.java) ?: ""
                            val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val dateStr = child.child("date").getValue(String::class.java) ?: LocalDate.now().toString()
                            val catStr = child.child("category").getValue(String::class.java) ?: ExpenseCategory.OTHER.name
                            fetchedExpenses.add(
                                Expense(id, name, amount, LocalDate.parse(dateStr), ExpenseCategory.valueOf(catStr))
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("Firebase", "Błąd parsowania wydatku: ${e.message}")
                        }
                    }
                    expensesList = fetchedExpenses
                    isLoading = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        }

        val goalsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    goalsList = emptyList()
                    isGoalsLoading = false
                } else {
                    val fetchedGoals = mutableListOf<SavingsGoal>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.key ?: continue
                            val name = child.child("name").getValue(String::class.java) ?: ""
                            val targetAmount = child.child("targetAmount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val currentAmount = child.child("currentAmount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val categoryStr = child.child("category").getValue(String::class.java) ?: ExpenseCategory.FOOD.name
                            val isCompleted = child.child("completed").getValue(Boolean::class.java) ?: false

                            val category = try {
                                ExpenseCategory.valueOf(categoryStr)
                            } catch (_: IllegalArgumentException) {
                                ExpenseCategory.FOOD
                            }

                            fetchedGoals.add(
                                SavingsGoal(
                                    id = id,
                                    name = name,
                                    targetAmount = targetAmount,
                                    currentAmount = currentAmount,
                                    category = category,
                                    isCompleted = isCompleted
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("Firebase", "Błąd parsowania celu: ${e.message}")
                        }
                    }
                    goalsList = fetchedGoals
                    isGoalsLoading = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isGoalsLoading = false
            }
        }

        expensesRef.addValueEventListener(expensesListener)
        goalsRef.addValueEventListener(goalsListener)

        onDispose {
            expensesRef.removeEventListener(expensesListener)
            goalsRef.removeEventListener(goalsListener)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarItems.forEach { screen ->
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chart.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                if (targetState.destination.route in detailRoutes) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    )
                } else {
                    fadeIn(animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (targetState.destination.route in detailRoutes) {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    )
                } else {
                    fadeOut(animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                if (initialState.destination.route in detailRoutes) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    )
                } else {
                    fadeIn(animationSpec = tween(300))
                }
            },
            popExitTransition = {
                if (initialState.destination.route in detailRoutes) {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    )
                } else {
                    fadeOut(animationSpec = tween(300))
                }
            }
        ) {
            composable(Screen.Chart.route) {
                ExpensesScreen(
                    expensesList = expensesList,
                    isLoading = isLoading,
                    initialBarFractions = savedChartFractions,
                    onInitialBarFractionsConsumed = { savedChartFractions = null },
                    onCategoryClick = { categoryName, currentBarFractions ->
                        savedChartFractions = currentBarFractions
                        navController.navigate(DetailRoutes.categoryDetails(categoryName))
                    }
                )
            }
            composable(Screen.Categories.route) {
                MonthlyChartScreen(
                    expensesList = expensesList,
                    isLoading = isLoading,
                    onExpandableCategoriesClick = { selectedMonthStr ->
                        navController.navigate(DetailRoutes.expandableCategories(selectedMonthStr))
                    }
                )
            }
            composable(Screen.ExpensesList.route) {
                ExpensesListScreen(
                    expensesList = expensesList,
                    isLoading = isLoading,
                    onExpenseClick = { expenseId ->
                        navController.navigate(DetailRoutes.expenseDetail(expenseId))
                    }
                )
            }
            composable(Screen.SavingsGoals.route) {
                SavingsGoalsScreen(goalsList = goalsList, isLoading = isGoalsLoading)
            }

            // --- EKRANY SZCZEGÓŁOWE (dawne osobne Activity) ---

            composable(
                route = DetailRoutes.CATEGORY_DETAILS,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: return@composable
                CategoryDetailsScreen(
                    categoryName = categoryName,
                    expensesList = expensesList,
                    isLoading = isLoading,
                    onExpenseClick = { expenseId ->
                        navController.navigate(DetailRoutes.expenseDetail(expenseId))
                    }
                )
            }

            composable(
                route = DetailRoutes.EXPANDABLE_CATEGORIES,
                arguments = listOf(
                    navArgument("selectedMonth") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val selectedMonthStr = backStackEntry.arguments?.getString("selectedMonth")
                ExpandableCategoriesScreen(
                    selectedMonthStr = selectedMonthStr,
                    expensesList = expensesList,
                    isLoading = isLoading,
                    onExpenseClick = { expenseId ->
                        navController.navigate(DetailRoutes.expenseDetail(expenseId))
                    }
                )
            }

            composable(
                route = DetailRoutes.EXPENSE_DETAIL,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                ExpenseDetailScreen(
                    expenseId = expenseId,
                    expensesList = expensesList,
                    isLoading = isLoading,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}