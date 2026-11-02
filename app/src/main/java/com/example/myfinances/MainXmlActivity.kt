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

    var cachedExpenses: List<Expense> = emptyList()
    var isDataLoaded = false

    private val dataListeners = mutableListOf<(List<Expense>) -> Unit>()

    fun addDataListener(listener: (List<Expense>) -> Unit) {
        dataListeners.add(listener)
        if (isDataLoaded) {
            listener.invoke(cachedExpenses)
        }
    }

    fun removeDataListener(listener: (List<Expense>) -> Unit) {
        dataListeners.remove(listener)
    }

    private var databaseRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_xml)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setupWithNavController(navController)

        fetchDataFromFirebase()
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
                    val currentMonth = LocalDate.now().monthValue
                    val currentYear = LocalDate.now().year

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

                            if (date.monthValue == currentMonth && date.year == currentYear) {
                                fetchedExpenses.add(Expense(id, name, amount, date, category))
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseXML", "Błąd parsowania: ${e.message}")
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

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { databaseRef?.removeEventListener(it) }
    }
}