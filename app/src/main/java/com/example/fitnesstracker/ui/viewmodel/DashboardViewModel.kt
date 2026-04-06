package com.example.fitnesstracker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * DashboardViewModel — Single source of truth for the Dashboard screen.
 *
 * Performance Optimizations:
 *
 *   1. Rotation-safe in-memory cache
 *      ViewModel survives configuration changes (rotation) by design.
 *      [_dataLoadedAt] tracks when data was last fetched. [fetchActivities]
 *      skips the network call if data was loaded less than [CACHE_TTL_MS] ago,
 *      meaning rotating the device or returning from sub-screens is instant.
 *
 *   2. Fetch-once-per-session
 *      The ON_RESUME lifecycle hook in DashboardActivity calls [fetchActivities].
 *      Previously this triggered a full network request on every return from any screen.
 *      Now it is a fast no-op if data was fetched recently. Only [forceRefresh] bypasses
 *      the cache (e.g. pull-to-refresh, after saving a new activity).
 *
 *   3. Retry with back-off
 *      Up to 3 attempts with 800 ms back-off before showing an error.
 *
 *   4. Hard 5-second timeout
 *      Spinner can never hang indefinitely — timeout resets to empty state.
 */
class DashboardViewModel(private val repository: DataRepository) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"

        /**
         * Cache Time-To-Live: 30 seconds.
         * After a successful fetch, any [fetchActivities] call within this window
         * is a no-op and returns immediately with the cached in-memory data.
         * Returning from Profile, Notifications, or any other sub-screen within
         * 30 s will be instant — zero network calls.
         */
        private const val CACHE_TTL_MS = 30_000L
    }

    // ── Reactive state observed by DashboardScreen ─────────────────────────────

    private val _activities    = MutableStateFlow<List<ActivityData>>(emptyList())
    val activities: StateFlow<List<ActivityData>> = _activities.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _totalCalories = MutableStateFlow(0)
    val totalCalories: StateFlow<Int> = _totalCalories.asStateFlow()

    private val _totalDuration = MutableStateFlow(0)
    val totalDuration: StateFlow<Int> = _totalDuration.asStateFlow()

    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _error         = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Cache bookkeeping ──────────────────────────────────────────────────────

    /**
     * Epoch-ms timestamp of the last successful data fetch.
     * 0L means "never fetched". Compared against [CACHE_TTL_MS] in [fetchActivities].
     */
    private var _dataLoadedAt: Long = 0L

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Fetches activities for [userId], respecting the [CACHE_TTL_MS] cache window.
     *
     * Called by [DashboardActivity] on every ON_RESUME lifecycle event.
     * If data was loaded recently, this is a fast no-op — returning from any sub-screen
     * will not trigger a redundant network call.
     *
     * @param userId  The authenticated user's ID. Must be > 0.
     * @param force   When true, bypasses the cache and always fetches fresh data.
     *                Set by the pull-to-refresh gesture or after saving a new activity.
     */
    fun fetchActivities(userId: Int, force: Boolean = false) {
        if (userId <= 0) {
            _error.value = "Session error: please log in again."
            _isLoading.value = false
            return
        }

        // ── Cache guard ───────────────────────────────────────────────────────
        // Skip network call if data is loaded and still within CACHE_TTL_MS.
        // This makes rotation, back-navigation, and sub-screen returns instant.
        val now      = System.currentTimeMillis()
        val dataFresh = _dataLoadedAt > 0L && (now - _dataLoadedAt) < CACHE_TTL_MS
        if (dataFresh && !force) {
            Log.d(TAG, "fetchActivities: cache hit (age ${now - _dataLoadedAt}ms < ${CACHE_TTL_MS}ms) — skipping network call")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null

            try {
                // Hard 5-second ceiling — spinner can NEVER hang beyond this
                val timedOut = withTimeoutOrNull(5_000L) {
                    var attempt = 1
                    var success = false

                    while (attempt <= 3 && !success) {
                        repository.getActivities(userId)
                            .onSuccess { response ->
                                val list = response.activities
                                _activities.value    = list
                                _totalSessions.value = list.size
                                _totalCalories.value = list.sumOf { it.calories }
                                _totalDuration.value = list.sumOf { it.duration }
                                _dataLoadedAt        = System.currentTimeMillis()  // stamp cache
                                success = true
                                Log.d(TAG, "fetchActivities: success (${list.size} activities) on attempt $attempt")
                            }
                            .onFailure { t ->
                                if (attempt < 3) {
                                    Log.w(TAG, "Attempt $attempt failed — retrying in 800 ms: ${t.message}")
                                    delay(800)
                                } else {
                                    // All retries exhausted — show empty state, not a spinner
                                    _activities.value    = emptyList()
                                    _totalSessions.value = 0
                                    _totalCalories.value = 0
                                    _totalDuration.value = 0
                                    _error.value = "Could not load activities (${t.localizedMessage ?: "Network error"})"
                                    Log.e(TAG, "All retries failed: ${t.message}")
                                }
                            }
                        attempt++
                    }
                } // end withTimeoutOrNull

                if (timedOut == null) {
                    // Timeout path — reset to safe empty state
                    _activities.value    = emptyList()
                    _totalSessions.value = 0
                    _totalCalories.value = 0
                    _totalDuration.value = 0
                    _error.value = "No data found. Check your connection and try again."
                    Log.e(TAG, "fetchActivities timed out after 5s")
                }
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.localizedMessage}"
                Log.e(TAG, "fetchActivities exception: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Forces a fresh network fetch, bypassing the [CACHE_TTL_MS] guard.
     * Call this after saving a new activity so the Dashboard reflects it immediately.
     */
    fun forceRefresh(userId: Int) {
        fetchActivities(userId, force = true)
    }

    /**
     * Marks the cache as stale without triggering a fetch.
     * Call from TrackingActivity (or AddActivity) just before finishing,
     * so the Dashboard's next ON_RESUME always fetches fresh data.
     */
    fun invalidateCache() {
        _dataLoadedAt = 0L
        Log.d(TAG, "Cache invalidated — next fetchActivities() will hit the network")
    }

    /** Clears the transient error state (consumed by the UI after showing a Toast). */
    fun clearError() {
        _error.value = null
    }

}
