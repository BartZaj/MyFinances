package com.example.myfinances

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate

class ExpandableCategoriesXmlActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expandable_categories)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadData()
    }

    private fun loadData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedExpenses = mutableListOf<Expense>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val amount = child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                    val dateStr = child.child("date").getValue(String::class.java) ?: LocalDate.now().toString()
                    val catStr = child.child("category").getValue(String::class.java) ?: ExpenseCategory.OTHER.name
                    fetchedExpenses.add(Expense(name, amount, LocalDate.parse(dateStr), ExpenseCategory.valueOf(catStr)))
                }

                // Grupowanie
                val grouped = fetchedExpenses.groupBy { it.category }

                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = CategoryExpandableAdapter(grouped)
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
            }
        })
    }
}

class CategoryExpandableAdapter(private val groupedExpenses: Map<ExpenseCategory, List<Expense>>) :
    RecyclerView.Adapter<CategoryExpandableAdapter.ViewHolder>() {

    private val categories = groupedExpenses.keys.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerLayout: RelativeLayout = view.findViewById(R.id.headerLayout)
        val colorIndicator: View = view.findViewById(R.id.colorIndicator)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCategoryTotal: TextView = view.findViewById(R.id.tvCategoryTotal)
        val ivArrow: android.widget.ImageView = view.findViewById(R.id.ivArrow) // Dodana strzałka
        val expandableContent: LinearLayout = view.findViewById(R.id.expandableContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expandable_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val expenses = groupedExpenses[category] ?: emptyList()
        val total = expenses.sumOf { it.amount.toDouble() }.toFloat()

        holder.tvCategoryName.text = category.displayName
        holder.tvCategoryTotal.text = "${total.toInt()} zł"

        // Ustawienie dynamicznego koloru z enum ExpenseCategory
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            setColor(category.color.toArgb()) // Przekonwertowanie koloru Compose na kolor Androida
        }
        holder.colorIndicator.background = shape

        holder.expandableContent.removeAllViews()

        for (expense in expenses) {
            val row = LinearLayout(holder.itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 8, 0, 8)
                }
            }

            val tvName = TextView(holder.itemView.context).apply {
                text = expense.name
                setTextColor(android.graphics.Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvAmount = TextView(holder.itemView.context).apply {
                text = "- ${expense.amount.toInt()} zł"
                setTextColor(android.graphics.Color.GRAY)
            }

            row.addView(tvName)
            row.addView(tvAmount)
            holder.expandableContent.addView(row)
        }

        // Mechanizm rozwijania i animacji strzałki
        var isExpanded = false
        holder.ivArrow.rotation = 0f // Reset rotacji po recyklingu widoku

        holder.headerLayout.setOnClickListener {
            isExpanded = !isExpanded
            holder.expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Płynny obrót strzałki o 180 stopni
            holder.ivArrow.animate()
                .rotation(if (isExpanded) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }

    override fun getItemCount() = categories.size
}