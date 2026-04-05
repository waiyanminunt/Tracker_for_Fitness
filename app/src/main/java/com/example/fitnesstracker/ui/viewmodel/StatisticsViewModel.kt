package com.example.fitnesstracker.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.ui.activities.TimeFilter

/**
 * StatisticsViewModel — scoped to StatisticsActivity's lifecycle.
 *
 * Holds the selectedFilter state so it survives configuration changes
 * (screen rotation, font-size change, etc.) without resetting.
 *
 * Also centralises all data-bucketing logic that was previously
 * scattered inside the CaloriesBarChart composable, making the
 * composable purely presentational (UI only).
 *
 * ─────────────────────────────────────────────────────────────────
 * Why NOT a NavGraph / Application-scoped ViewModel?
 * ─────────────────────────────────────────────────────────────────
 * This app uses Activity-based navigation (each tab launches a new
 * Activity via startActivity()). There is no shared NavHost or
 * NavGraph to scope a cross-screen ViewModel to.
 *
 * An Application-scoped ViewModel would share state across ALL
 * Activities but introduces memory leaks and makes lifecycle
 * reasoning harder — overkill for a single screen preference.
 *
 * The correct solution for this architecture is:
 *  • StatisticsActivity owns a StatisticsViewModel (survives rotation)
 *  • DashboardActivity owns nothing (its filter is local, resets on
 *    re-entry, which is the expected UX for a tab-based app)
 *
 * If you later migrate to a single-Activity + NavHost architecture,
 * simply scope the ViewModel to the NavBackStackEntry or the Activity.
 * ─────────────────────────────────────────────────────────────────
 */
class StatisticsViewModel : ViewModel() {

    // ── Single source of truth for the time filter ───────────────────────────
    // Using Compose mutableStateOf so Composables reading this state
    // automatically recompose when it changes — no StateFlow.collectAsState()
    // boilerplate needed.
    var selectedFilter: TimeFilter by mutableStateOf(TimeFilter.ALL_TIME)
        private set   // only ViewModel can write; Composables call selectFilter()

    /** Called from the UI when the user taps a filter pill. */
    fun selectFilter(filter: TimeFilter) {
        selectedFilter = filter
    }

    // ── Mock data sets (three distinct periods) ──────────────────────────────
    // These are the illustrative data shown when the user has no real
    // activities in the selected period. The composable falls back to these.

    data class ChartDataSet(
        val labels: List<String>,
        val values: IntArray
    )

    val mockWeekly = ChartDataSet(
        labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
        values = intArrayOf(320, 480, 210, 550, 390, 620, 445)
    )

    val mockMonthly = ChartDataSet(
        labels = listOf("W1", "W2", "W3", "W4"),
        values = intArrayOf(1840, 2650, 980, 3120)
    )

    val mockAllTime = ChartDataSet(
        labels = listOf("Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec"),
        values = intArrayOf(4200,3800,5100,4700,6200,5800,
                            7100,6600,5300,4900,6800,7500)
    )

    // ── Data-bucketing helpers moved out of the Composable ───────────────────
    // Each function maps raw ActivityData from the API into the correct
    // bucket array, then falls back to mock data if the result is all zeros.

    fun computeWeeklyBuckets(activities: List<ActivityData>): IntArray {
        val buckets = IntArray(7)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        activities.forEach { act ->
            try {
                val d   = sdf.parse(act.created_at) ?: return@forEach
                val cal = java.util.Calendar.getInstance().apply { time = d }
                // Sun=1..Sat=7 → remap to Mon=0..Sun=6
                val idx = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                buckets[idx] += act.calories
            } catch (_: Exception) {}
        }
        return if (buckets.all { it == 0 }) mockWeekly.values else buckets
    }

    fun computeMonthlyBuckets(activities: List<ActivityData>): IntArray {
        val buckets = IntArray(4)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val now = java.util.Calendar.getInstance()
        activities.forEach { act ->
            try {
                val d   = sdf.parse(act.created_at) ?: return@forEach
                val cal = java.util.Calendar.getInstance().apply { time = d }
                val daysAgo = ((now.timeInMillis - cal.timeInMillis) / 86_400_000).toInt()
                val week    = (daysAgo / 7).coerceIn(0, 3)
                buckets[3 - week] += act.calories
            } catch (_: Exception) {}
        }
        return if (buckets.all { it == 0 }) mockMonthly.values else buckets
    }

    fun computeAllTimeBuckets(activities: List<ActivityData>): IntArray {
        val buckets = IntArray(12)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        activities.forEach { act ->
            try {
                val d   = sdf.parse(act.created_at) ?: return@forEach
                val cal = java.util.Calendar.getInstance().apply { time = d }
                buckets[cal.get(java.util.Calendar.MONTH)] += act.calories
            } catch (_: Exception) {}
        }
        return if (buckets.all { it == 0 }) mockAllTime.values else buckets
    }

    /**
     * Returns the correct (labels, values) pair for the currently
     * selected filter — the composable calls this instead of doing
     * the when() switch itself.
     */
    fun resolveChartData(activities: List<ActivityData>): ChartDataSet {
        return when (selectedFilter) {
            TimeFilter.WEEKLY   -> ChartDataSet(mockWeekly.labels,  computeWeeklyBuckets(activities))
            TimeFilter.MONTHLY  -> ChartDataSet(mockMonthly.labels, computeMonthlyBuckets(activities))
            TimeFilter.ALL_TIME -> ChartDataSet(mockAllTime.labels, computeAllTimeBuckets(activities))
        }
    }
}
