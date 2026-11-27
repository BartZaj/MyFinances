package com.example.myfinances

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate

class MainXmlActivity : AppCompatActivity() {

    // --- WYDATKI ---
    var cachedExpenses: List<Expense> = emptyList()
    var isDataLoaded = false
    private val dataListeners = mutableListOf<(List<Expense>) -> Unit>()
    private var databaseRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    // --- CELE OSZCZĘDNOŚCIOWE ---
    var cachedGoals: List<SavingsGoal> = emptyList()
    var isGoalsLoaded = false
    private val goalsListeners = mutableListOf<(List<SavingsGoal>) -> Unit>()
    private var goalsRef: DatabaseReference? = null
    private var goalsEventListener: ValueEventListener? = null

    fun addDataListener(listener: (List<Expense>) -> Unit) {
        dataListeners.add(listener)
        if (isDataLoaded) {
            listener.invoke(cachedExpenses)
        }
    }

    fun removeDataListener(listener: (List<Expense>) -> Unit) {
        dataListeners.remove(listener)
    }

    fun addGoalsListener(listener: (List<SavingsGoal>) -> Unit) {
        goalsListeners.add(listener)
        if (isGoalsLoaded) {
            listener.invoke(cachedGoals)
        }
    }

    fun removeGoalsListener(listener: (List<SavingsGoal>) -> Unit) {
        goalsListeners.remove(listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_xml)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setupWithNavController(navController)

        fetchDataFromFirebase()
        fetchGoalsFromFirebase()
    }

    private fun fetchDataFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isDataLoaded = true
            return
        }

        databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    cachedExpenses = emptyList()
                    isDataLoaded = true
                } else {
                    val fetchedExpenses = mutableListOf<Expense>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.key ?: ""
                            val name = child.child("name").getValue(String::class.java) ?: ""
                            val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                            val dateStr = child.child("date").getValue(String::class.java) ?: ""
                            val categoryStr = child.child("category").getValue(String::class.java) ?: ""

                            val date = LocalDate.parse(dateStr)
                            val category = ExpenseCategory.valueOf(categoryStr)

                            fetchedExpenses.add(Expense(id, name, amount, date, category))
                        } catch (e: Exception) {
                            Log.e("FirebaseXML", "Błąd parsowania wydatku: ${e.message}")
                        }
                    }
                    cachedExpenses = fetchedExpenses
                    isDataLoaded = true
                }

                dataListeners.forEach { it.invoke(cachedExpenses) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseXML", "Błąd: ${error.message}")
                isDataLoaded = true
            }
        }
        databaseRef!!.addValueEventListener(valueEventListener!!)
    }

    private fun fetchGoalsFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isGoalsLoaded = true
            return
        }

        goalsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("SavingsGoals")

        goalsEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    cachedGoals = emptyList()
                    isGoalsLoaded = true
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
                            Log.e("FirebaseXML", "Błąd parsowania celu: ${e.message}")
                        }
                    }
                    cachedGoals = fetchedGoals
                    isGoalsLoaded = true
                }

                goalsListeners.forEach { it.invoke(cachedGoals) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseXML", "Błąd pobierania celów: ${error.message}")
                isGoalsLoaded = true
            }
        }
        goalsRef!!.addValueEventListener(goalsEventListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { databaseRef?.removeEventListener(it) }
        goalsEventListener?.let { goalsRef?.removeEventListener(it) }
    }
}