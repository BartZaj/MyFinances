package com.example.myfinances

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Trace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MonthlyChartFragment : Fragment() {

    private lateinit var chartView: MonthsChartView
    private lateinit var tvToggleBar: TextView
    private lateinit var tvToggleLine: TextView
    private var isBarChart = true

    // Callback odświeżający dane
    private val dataListener: (List<Expense>) -> Unit = { expenses ->
        processExpensesForChart(expenses)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Trace.beginSection("XMLScreen_CategoriesLauncher")
        val view = inflater.inflate(R.layout.fragment_monthly_chart, container, false)

        chartView = view.findViewById(R.id.chartView)
        tvToggleBar = view.findViewById(R.id.tvToggleBar)
        tvToggleLine = view.findViewById(R.id.tvToggleLine)

        setupToggle()

        // Podpinamy Listenera do MainXmlActivity
        (activity as? MainXmlActivity)?.addDataListener(dataListener)

        chartView.setOnMonthClickListener { selectedMonth ->
            val intent = Intent(requireContext(), ExpandableCategoriesXmlActivity::class.java).apply {
                putExtra("SELECTED_MONTH", selectedMonth.toString())
            }
            startActivity(intent)
        }

        Trace.endSection()
        return view
    }

    private fun setupToggle() {
        tvToggleBar.setOnClickListener {
            if (!isBarChart) {
                isBarChart = true
                tvToggleBar.setBackgroundResource(R.drawable.toggle_selected)
                tvToggleBar.setTextColor(Color.WHITE)
                tvToggleLine.setBackgroundColor(Color.TRANSPARENT)
                tvToggleLine.setTextColor(Color.parseColor("#555555"))
                chartView.setChartType(true)
            }
        }

        tvToggleLine.setOnClickListener {
            if (isBarChart) {
                isBarChart = false
                tvToggleLine.setBackgroundResource(R.drawable.toggle_selected)
                tvToggleLine.setTextColor(Color.WHITE)
                tvToggleBar.setBackgroundColor(Color.TRANSPARENT)
                tvToggleBar.setTextColor(Color.parseColor("#555555"))
                chartView.setChartType(false)
            }
        }
    }

    private fun processExpensesForChart(expensesList: List<Expense>) {
        val currentMonth = YearMonth.now()
        val last6Months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val formatter = DateTimeFormatter.ofPattern("MM/yy")

        val chartData = last6Months.map { month ->
            val total = expensesList.filter { YearMonth.from(it.date) == month }
                .sumOf { it.amount.toDouble() }.toFloat()
            Pair(month.format(formatter), total)
        }

        val rawMonths = last6Months.toList()
        chartView.setData(chartData, rawMonths)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeDataListener(dataListener)
    }
}