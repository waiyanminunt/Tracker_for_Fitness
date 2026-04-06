package com.example.fitnesstracker.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.ui.activities.TimeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * StatisticsViewModel — Drives the Statistics screen.
 *
 * Data Integrity Guarantee:
 *   ZERO mock / fallback / fake data. Every bar in the chart, every number in the
 *   summary cards, is derived exclusively from real database rows fetched via
 *   [DataRepository.getActivities].
 *
 *   If a time bucket has no activities, its bar height is 0. If the whole chart
 *   is empty, all bars are 0. This is the correct behaviour.
 *
 * Chart Bug (Fixed):
 *   The old code had three fallback guards:
 *     if (buckets.all { it == 0 }) return mockXxx.values
 *   These triggered whenever the selected time filter (Weekly / Monthly) excluded
 *   all DB activities, producing fictional bars for months that had no data.
 *   All three guards have been removed. 0 means 0.
 */
class StatisticsViewModel(private val repository: DataRepository) : ViewModel() {

    private val _activities = MutableStateFlow<List<ActivityData>>(emptyList())
    val activities: StateFlow<List<ActivityData>> = _activities.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    var selectedFilter: TimeFilter by mutableStateOf(TimeFilter.ALL_TIME)
        private set

    fun selectFilter(filter: TimeFilter) {
        selectedFilter = filter
    }

    /**
     * Loads all historical activities for [userId].
     * Spinner Safety: times out after 5 s; [isLoading] is always reset in finally.
     */
    fun fetchActivities(userId: Int) {
        if (userId <= 0) {
            _isLoading.value = false
            _error.value = "Session error: please log in again."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withTimeoutOrNull(5_000L) {
                    repository.getActivities(userId)
                }
                when {
                    result == null -> {
                        _activities.value = emptyList()
                        _error.value = "Statistics load timed out. Check your connection."
                    }
                    else -> result
                        .onSuccess  { response -> _activities.value = response.activities }
                        .onFailure  { t ->
                            _activities.value = emptyList()
                            _error.value = "Could not load statistics: ${t.localizedMessage ?: "Connection error"}"
                        }
                }
            } catch (e: Exception) {
                _activities.value = emptyList()
                _error.value = "Unexpected error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Chart data model ───────────────────────────────────────────────────────

    /** Holds the labels and calorie-sum values for the bar chart. */
    data class ChartDataSet(val labels: List<String>, val values: IntArray)

    /**
     * Resolves the correct chart dataset for the [selectedFilter].
     * All buckets default to 0. If an activity cannot be parsed, it is skipped.
     * NO mock data is used under any circumstances.
     */
    fun resolveChartData(activities: List<ActivityData>): ChartDataSet {
        return when (selectedFilter) {
            TimeFilter.WEEKLY   -> ChartDataSet(WEEKLY_LABELS,    computeWeeklyBuckets(activities))
            TimeFilter.MONTHLY  -> ChartDataSet(MONTHLY_LABELS,   computeMonthlyBuckets(activities))
            TimeFilter.ALL_TIME -> ChartDataSet(ALL_TIME_LABELS,   computeAllTimeBuckets(activities))
        }
    }

    // ── Bucket computation ─────────────────────────────────────────────────────

    /**
     * Sums calories per day-of-week (Mon=0 … Sun=6) for activities in [activities].
     * Activities that fall outside the current week will still be included IF
     * the caller already filtered by WEEKLY before passing the list in.
     * Chart labels: Mon, Tue, Wed, Thu, Fri, Sat, Sun.
     */
    private fun computeWeeklyBuckets(activities: List<ActivityData>): IntArray {
        val buckets = IntArray(7)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        activities.forEach { act ->
            try {
                val parsedDate = sdf.parse(act.created_at) ?: return@forEach
                val cal = Calendar.getInstance().apply { time = parsedDate }
                // DAY_OF_WEEK: Sun=1, Mon=2 … Sat=7 → convert to Mon=0 … Sun=6
                val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                buckets[idx] += act.calories
            } catch (_: Exception) { /* skip unparseable rows */ }
        }
        // Return real zeros — do NOT substitute mock data
        return buckets
    }

    /**
     * Sums calories per ISO week-of-month (W4=most recent … W1=oldest in month).
     * Activities outside the current month are included only if already filtered.
     * Chart labels: W1, W2, W3, W4.
     */
    private fun computeMonthlyBuckets(activities: List<ActivityData>): IntArray {
        val buckets = IntArray(4)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val now = Calendar.getInstance()
        activities.forEach { act ->
            try {
                val parsedDate = sdf.parse(act.created_at) ?: return@forEach
                val cal = Calendar.getInstance().apply { time = parsedDate }
                val daysAgo = ((now.timeInMillis - cal.timeInMillis) / 86_400_000L).toInt()
                val week = (daysAgo / 7).coerceIn(0, 3)
                buckets[3 - week] += act.calories
            } catch (_: Exception) { /* skip unparseable rows */ }
        }
        // Return real zeros — do NOT substitute mock data
        return buckets
    }

    /**
     * Sums calories per calendar month (Jan=0 … Dec=11).
     * Only activities from the CURRENT YEAR are counted per bucket, so past-year
     * data does not inflate the wrong month.
     * Chart labels: Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec.
     */
    private fun computeAllTimeBuckets(activities: List<ActivityData>): IntArray {
        val buckets = IntArray(12)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        activities.forEach { act ->
            try {
                val parsedDate = sdf.parse(act.created_at) ?: return@forEach
                val cal = Calendar.getInstance().apply { time = parsedDate }
                // Only count activities from the current year to prevent
                // old data from polluting the current-year chart
                if (cal.get(Calendar.YEAR) == currentYear) {
                    buckets[cal.get(Calendar.MONTH)] += act.calories
                }
            } catch (_: Exception) { /* skip unparseable rows */ }
        }
        // Return real zeros — do NOT substitute mock data
        return buckets
    }

    companion object {
        val WEEKLY_LABELS   = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val MONTHLY_LABELS  = listOf("W1", "W2", "W3", "W4")
        val ALL_TIME_LABELS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}