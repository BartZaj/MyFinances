package com.example.myfinances

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            progressBar.visibility = View.GONE
            return
        }
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
    private val expandedCategories = mutableSetOf<ExpenseCategory>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerLayout: RelativeLayout = view.findViewById(R.id.headerLayout)
        val colorIndicator: View = view.findViewById(R.id.colorIndicator)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCategoryTotal: TextView = view.findViewById(R.id.tvCategoryTotal)
        val ivArrow: ImageView = view.findViewById(R.id.ivArrow)
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

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            setColor(category.color.toArgb())
        }
        holder.colorIndicator.background = shape

        holder.expandableContent.removeAllViews()

        for (expense in expenses) {
            val row = LinearLayout(holder.itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }

                // Dodajemy efekt fali (ripple) przy kliknięciu, aby zachowywało się jak w Compose
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                isClickable = true
                isFocusable = true
            }

            val tvName = TextView(holder.itemView.context).apply {
                text = expense.name
                setTextColor(android.graphics.Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvAmount = TextView(holder.itemView.context).apply {
                text = "- ${expense.amount.toInt()} zł"
                setTextColor(Color.GRAY)
            }

            row.addView(tvName)
            row.addView(tvAmount)

            // --- TUTAJ JEST LOGIKA KLIKANIA WZOROWANA NA COMPOSE ---
            row.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            }

            holder.expandableContent.addView(row)
        }

        // --- NAPRAWIONY FRAGMENT ZARZĄDZANIA STANEM ROZWINIĘCIA ---

        val isExpanded = expandedCategories.contains(category)

        holder.expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivArrow.rotation = if (isExpanded) 180f else 0f

        holder.headerLayout.setOnClickListener {
            val currentlyExpanded = expandedCategories.contains(category)

            val parent = holder.itemView.parent as? ViewGroup
            if (parent != null) {
                android.transition.TransitionManager.beginDelayedTransition(
                    parent,
                    android.transition.AutoTransition().apply { duration = 250 }
                )
            }

            if (currentlyExpanded) {
                // Zwijanie: usuwamy z pamięci i aktualizujemy UI
                holder.expandableContent.visibility = View.GONE
                holder.ivArrow.animate().rotation(0f).setDuration(250).start()
            } else {
                expandedCategories.add(category)
                holder.expandableContent.visibility = View.VISIBLE
                holder.ivArrow.animate().rotation(180f).setDuration(250).start()
            }
        }
    }

    override fun getItemCount() = categories.size
}