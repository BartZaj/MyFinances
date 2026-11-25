package com.example.myfinances

import android.graphics.Color
import android.os.Bundle
import android.os.Trace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MonthlyChartFragment : Fragment() {

    private lateinit var chartView: MonthsChartView
    private lateinit var tvToggleBar: TextView
    private lateinit var tvToggleLine: TextView
    private var isBarChart = true
    private var skipNextAnimation = false

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
        applyToggleUi(animateChart = false)

        chartView.setOnMonthClickListener { month ->
            skipNextAnimation = true
            findNavController().navigate(
                R.id.expandableCategoriesXmlFragment,
                bundleOf("SELECTED_MONTH" to month.toString()),
                slideNavOptions
            )
        }

        // Podpinamy Listenera do MainXmlActivity
        (activity as? MainXmlActivity)?.addDataListener(dataListener)

        Trace.endSection()
        return view
    }

    private fun setupToggle() {
        tvToggleBar.setOnClickListener {
            if (!isBarChart) {
                isBarChart = true
                applyToggleUi(animateChart = true)
            }
        }

        tvToggleLine.setOnClickListener {
            if (isBarChart) {
                isBarChart = false
                applyToggleUi(animateChart = true)
            }
        }
    }

    private fun applyToggleUi(animateChart: Boolean) {
        if (isBarChart) {
            tvToggleBar.setBackgroundResource(R.drawable.toggle_selected)
            tvToggleBar.setTextColor(Color.WHITE)
            tvToggleLine.setBackgroundColor(Color.TRANSPARENT)
            tvToggleLine.setTextColor(Color.parseColor("#555555"))
        } else {
            tvToggleLine.setBackgroundResource(R.drawable.toggle_selected)
            tvToggleLine.setTextColor(Color.WHITE)
            tvToggleBar.setBackgroundColor(Color.TRANSPARENT)
            tvToggleBar.setTextColor(Color.parseColor("#555555"))
        }
        chartView.setChartType(isBarChart, animate = animateChart)
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
        val shouldAnimate = !skipNextAnimation
        skipNextAnimation = false
        chartView.setData(chartData, rawMonths, animate = shouldAnimate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainXmlActivity)?.removeDataListener(dataListener)
    }
}