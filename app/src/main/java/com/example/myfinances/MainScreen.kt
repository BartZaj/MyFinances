package com.example.myfinances

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chart : Screen("chart", "Wykres", Icons.Filled.PieChart)
    object Categories : Screen("categories", "Kategorie", Icons.Filled.List)
}

 // Wymagane dla LocalRippleConfiguration
 @OptIn(ExperimentalMaterial3Api::class)
 @Composable
 fun MainScreenHost() {
     val navController = rememberNavController()
     val navBackStackEntry by navController.currentBackStackEntryAsState()
     val currentRoute = navBackStackEntry?.destination?.route

     // 1. DODANE: Stany trzymające dane niezależnie od otwartej zakładki
     var expensesList by remember { mutableStateOf<List<Expense>>(emptyList()) }
     var isLoading by remember { mutableStateOf(true) }

     // 2. DODANE: Pobieranie z Firebase przeniesione z ExpensesScreen
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
                             fetchedExpenses.add(Expense(id, name, amount, LocalDate.parse(dateStr), ExpenseCategory.valueOf(catStr)))
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
         databaseRef.addValueEventListener(listener)
         onDispose { databaseRef.removeEventListener(listener) }
     }

     Scaffold(
// ... reszta kodu paska dolnego i NavHost pozostaje bez zmian ...
        bottomBar = {
            NavigationBar {
                val items = listOf(Screen.Chart, Screen.Categories)
                items.forEach { screen ->

                    // Poprawne wyłączenie fali kliknięcia w Material 3
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chart.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
            },
            popExitTransition = {
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            }
        ) {
// fragment kodu w NavHost w MainScreenHost:
            composable(Screen.Chart.route) {
                ExpensesScreen(expensesList = expensesList, isLoading = isLoading)
            }
            composable(Screen.Categories.route) {
                // DODANE PARAMETRY:
                MonthlyChartScreen(expensesList = expensesList, isLoading = isLoading)
            }
        }
    }
}
