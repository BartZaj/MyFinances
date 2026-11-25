package com.example.myfinances

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.YearMonth

class ExpandableCategoriesXmlFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var targetMonth: YearMonth? = null
    private val expandedCategories = mutableSetOf<ExpenseCategory>()

    private val dataListener: (List<Expense>) -> Unit = { allExpenses ->
        if (isAdded) {
            val filtered = allExpenses.filter {
                targetMonth == null || YearMonth.from(it.date) == targetMonth
            }
            val grouped = filtered.groupBy { it.category }

            progressBar.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            val currentAdapter = recyclerView.adapter as? CategoryExpandableAdapter
            if (currentAdapter == null) {
                recyclerView.adapter = CategoryExpandableAdapter(grouped, expandedCategories) { expenseId ->
                    findNavController().navigate(
                        R.id.expenseDetailXmlFragment,
                        bundleOf("EXTRA_ID" to expenseId),
                        slideNavOptions
                    )
                }
            } else {
                currentAdapter.updateData(grouped)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getStringArrayList("EXPANDED_CATEGORIES")?.forEach { name ->
            runCatching { ExpenseCategory.valueOf(name) }.getOrNull()?.let {
                expandedCategories.add(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(
            "EXPANDED_CATEGORIES",
            ArrayList(expandedCategories.map { it.name })
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_expandable_categories, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val selectedMonthStr = arguments?.getString("SELECTED_MONTH")
        targetMonth = selectedMonthStr?.let { YearMonth.parse(it) }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainXmlActivity
        if (mainActivity?.isDataLoaded == false) {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
        mainActivity?.addDataListener(dataListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeDataListener(dataListener)
    }
}

class CategoryExpandableAdapter(
    private var groupedExpenses: Map<ExpenseCategory, List<Expense>>,
    private val expandedCategories: MutableSet<ExpenseCategory>,
    private val onExpenseClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryExpandableAdapter.ViewHolder>() {

    private val categoryOrder = listOf("FOOD", "TRANSPORT", "ENTERTAINMENT", "BILLS", "CLOTHING", "HEALTH", "OTHER")

    private fun sortCategories(keys: Set<ExpenseCategory>): List<ExpenseCategory> {
        return keys.sortedBy { category ->
            val index = categoryOrder.indexOf(category.name)
            if (index == -1) Int.MAX_VALUE else index
        }
    }

    private var categories = sortCategories(groupedExpenses.keys)

    fun updateData(newGroupedExpenses: Map<ExpenseCategory, List<Expense>>) {
        this.groupedExpenses = newGroupedExpenses
        this.categories = sortCategories(newGroupedExpenses.keys)
        expandedCategories.retainAll(categories.toSet())
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardLayout: LinearLayout = view.findViewById(R.id.cardLayout)
        val headerLayout: RelativeLayout = view.findViewById(R.id.headerLayout)
        val iconCircleBackground: View = view.findViewById(R.id.iconCircleBackground)
        val iconIndicator: ImageView = view.findViewById(R.id.iconIndicator)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCategoryTotal: TextView = view.findViewById(R.id.tvCategoryTotal)
        val ivArrow: ImageView = view.findViewById(R.id.ivArrow)
        val expandableContent: LinearLayout = view.findViewById(R.id.expandableContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expandable_category, parent, false)
        return ViewHolder(view)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.expandableContent.visibility = View.GONE
        holder.expandableContent.removeAllViews()
        holder.ivArrow.rotation = 0f
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val expenses = groupedExpenses[category]?.sortedWith(
            compareByDescending<Expense> { it.date }
                .thenByDescending { it.id }
        ) ?: emptyList()
        val total = expenses.sumOf { it.amount.toDouble() }.toFloat()

        holder.tvCategoryName.text = category.displayName
        holder.tvCategoryTotal.text = String.format("%.2f zł", total)

        val baseColor = category.color.toArgb()
        val composeLikeBackgroundColor = ColorUtils.setAlphaComponent(baseColor, (255 * 0.18).toInt())

        val cardShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 64f
            setColor(composeLikeBackgroundColor)
        }
        holder.cardLayout.background = cardShape

        val circleShape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(baseColor)
        }
        holder.iconCircleBackground.background = circleShape

        holder.tvCategoryName.setTextColor(baseColor)
        holder.tvCategoryTotal.setTextColor(baseColor)
        holder.ivArrow.setColorFilter(baseColor)

        val iconRes = when (category.name) {
            "FOOD" -> R.drawable.baseline_directions_car_24
            "TRANSPORT" -> R.drawable.baseline_directions_car_24
            "ENTERTAINMENT" -> R.drawable.baseline_directions_car_24
            "CLOTHING" -> R.drawable.baseline_directions_car_24
            "HEALTH" -> R.drawable.baseline_directions_car_24
            "BILLS" -> R.drawable.baseline_directions_car_24
            else -> R.drawable.baseline_directions_car_24
        }
        holder.iconIndicator.setImageResource(iconRes)
        holder.iconIndicator.setColorFilter(Color.WHITE)

        holder.expandableContent.removeAllViews()

        for (expense in expenses) {
            val row = LinearLayout(holder.itemView.context).apply {
                id = View.generateViewId()
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 20)
                }

                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                isClickable = true
                isFocusable = true
            }

            val leftLayout = LinearLayout(holder.itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val expenseDot = View(holder.itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    setMargins(0, 0, 32, 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(baseColor)
                }
            }

            val textCol = LinearLayout(holder.itemView.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tvName = TextView(holder.itemView.context).apply {
                text = expense.name
                setTextColor(Color.DKGRAY)
                textSize = 16f
            }

            val tvDate = TextView(holder.itemView.context).apply {
                text = expense.date.toString()
                setTextColor(Color.GRAY)
                textSize = 12f
            }

            textCol.addView(tvName)
            textCol.addView(tvDate)

            leftLayout.addView(expenseDot)
            leftLayout.addView(textCol)

            val tvAmount = TextView(holder.itemView.context).apply {
                text = String.format("- %.2f zł", expense.amount)
                setTextColor(baseColor)
                textSize = 16f
            }

            row.addView(leftLayout)
            row.addView(tvAmount)

            row.setOnClickListener {
                onExpenseClick(expense.id)
            }

            holder.expandableContent.addView(row)
        }

        val isExpanded = expandedCategories.contains(category)

        holder.expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivArrow.rotation = if (isExpanded) 180f else 0f

        holder.headerLayout.setOnClickListener {
            val currentlyExpanded = expandedCategories.contains(category)
            val parent = holder.itemView.parent as? ViewGroup

            if (parent != null) {
                val customTransition = TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    duration = 250

                    addTransition(ChangeBounds())

                    val incomingCardsFade = Fade(Fade.IN).apply {
                        startDelay = 100
                        duration = 150
                        excludeTarget(holder.expandableContent, true)
                    }
                    addTransition(incomingCardsFade)

                    val fade = Fade().apply {
                        addTarget(holder.expandableContent)
                        for (i in 0 until holder.expandableContent.childCount) {
                            addTarget(holder.expandableContent.getChildAt(i))
                        }
                    }
                    addTransition(fade)
                }

                TransitionManager.beginDelayedTransition(parent, customTransition)
            }

            if (currentlyExpanded) {
                expandedCategories.remove(category)
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