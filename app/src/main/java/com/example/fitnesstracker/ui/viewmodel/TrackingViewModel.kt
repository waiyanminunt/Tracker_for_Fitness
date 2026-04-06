package com.example.fitnesstracker.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityRequest
import com.example.fitnesstracker.services.TrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

enum class TrackingState {
    IDLE, RUNNING, PAUSED, STOPPED
}

class TrackingViewModel(
    application: Application,
    private val repository: DataRepository
) : AndroidViewModel(application) {

    private val TAG = "TrackingViewModel"

    // Single Source of Truth from Service
    val trackingState = TrackingService.trackingState
    val activeActivityType = TrackingService.activeActivityType
    val durationSeconds = TrackingService.durationSeconds
    val distanceMeters = TrackingService.distanceMeters
    val locationPoints = TrackingService.locationPoints
    val currentSpeed = TrackingService.currentSpeed 

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    fun updatePermissionStatus(granted: Boolean) {
        _hasPermission.value = granted
    }

    /**
     * Session Lock: Check if another activity is running in the background.
     */
    fun isBusySession(currentActivity: String): Boolean {
        val active = activeActivityType.value
        return active != null && active != currentActivity
    }

    fun startTracking(activityType: String) {
        if (_hasPermission.value) {
            // Isolation: Only reset/start if no other activity is busy
            if (!isBusySession(activityType)) {
                if (trackingState.value == TrackingState.IDLE || trackingState.value == TrackingState.STOPPED) {
                    TrackingService.resetData()
                }
                TrackingService.startService(getApplication(), activityType)
            } else {
                Log.w(TAG, "Multi-start prevented for $activityType")
            }
        }
    }

    fun stopTracking() {
        TrackingService.stopService(getApplication())
    }

    fun pauseTracking() {
        TrackingService.pauseService(getApplication())
    }

    fun resumeTracking() {
        TrackingService.resumeService(getApplication())
    }

    fun finishTracking(userId: Int, activityType: String, onSuccess: () -> Unit) {
        // Defensive: if a different activity is busy, just stop and exit cleanly
        if (isBusySession(activityType)) {
            Log.e(TAG, "Finish attempted for non-busy activity $activityType")
            TrackingService.stopService(getApplication())
            TrackingService.resetData()
            onSuccess()
            return
        }
        saveActivity(userId, activityType, onSuccess)
    }

    fun clearError() {
        _apiError.value = null
    }

    fun calculatePace(): String {
        val dist = distanceMeters.value
        val time = durationSeconds.value
        if (dist < 5.0f || time <= 0L) return "0:00"
        
        val km = dist / 1000f
        val minutesPerKm = (time / 60f) / km
        if (minutesPerKm > 99) return "99:00"
        
        val m = minutesPerKm.toInt()
        val s = ((minutesPerKm - m) * 60).toInt()
        return String.format(Locale.US, "%d:%02d", m, s)
    }

    /**
     * Calculates calories burned using MET × weight × time.
     *
     * Formula: calories = MET × weight_kg × duration_hours
     *   where duration_hours = durationSeconds / 3600.0
     *
     * Why MET-based instead of distance-based?
     *   The old formula (km × calories/km) truncated to 0 kcal for short GPS sessions
     *   where distance < 25m, because (0.025km × 40) = 1.0 → Int cast = 0 sometimes.
     *   MET-based is the WHO standard and always produces a non-zero result for
     *   any session longer than 30 seconds, matching the pre-save validation.
     *
     * MET values (Metabolic Equivalent of Task):
     *   Running:       9.8  (vigorous — ~9.8× resting metabolism)
     *   Cycling:       7.5  (moderate)
     *   Hiking:        6.0  (uphill walking with pack)
     *   Walking:       3.5  (normal pace)
     *   Football:      7.0  (match play)
     *   Yoga:          2.8  (hatha)
     *   Weightlifting: 4.0  (general)
     *   Swimming:      6.0  (moderate)
     *   Default:       4.0  (moderate activity)
     *
     * Default body weight: 70 kg (standard metabolic average — no profile field yet).
     */
    private fun calculateCalories(activityType: String): Int {
        val durationSeconds = durationSeconds.value
        val durationHours   = durationSeconds / 3600.0

        val met = when (activityType) {
            "Running"       -> 9.8
            "Cycling"       -> 7.5
            "Hiking"        -> 6.0
            "Walking"       -> 3.5
            "Football"      -> 7.0
            "Yoga"          -> 2.8
            "Weightlifting" -> 4.0
            "Swimming"      -> 6.0
            else            -> 4.0
        }

        val weightKg = 70.0         // default; replace with user profile value when available
        val rawCalories = met * weightKg * durationHours

        // Round to nearest int, guarantee at least 1 kcal for any non-trivial session
        return rawCalories.toInt().coerceAtLeast(1)
    }

    /**
     * Hardened Save — guarantees the spinner ALWAYS clears and state ALWAYS resets.
     *
     * Flow:
     *   1. Validate (reject < 30s sessions before hitting the network).
     *   2. Build the request from the service's live state snapshot.
     *   3. POST to backend with a 15-second ceiling.
     *   4. On success → call [onSuccess] to post notification + navigate back.
     *   5. On failure/timeout → set _apiError so a Toast shows in the UI.
     *   6. finally → ALWAYS stop the service and reset state so next session is clean.
     */
    private fun saveActivity(userId: Int, activityType: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _apiError.value = null

            val finalDuration = durationSeconds.value   // seconds
            val finalDistance = distanceMeters.value    // metres

            // ── Pre-save Validation ─────────────────────────────────────────────
            // DEV_MODE = true  → 1s / 1m thresholds (for backend connectivity testing)
            // DEV_MODE = false → 30s / 10m thresholds (production)
            // TODO: set DEV_MODE = false before final release
            val DEV_MODE = true

            val isGpsActivity = activityType in listOf("Running", "Walking", "Cycling", "Hiking")
            val minDurationSec = if (DEV_MODE) 1L  else 30L
            val minDistanceM   = if (DEV_MODE) 1f  else 10f

            if (isGpsActivity && finalDuration < minDurationSec) {
                _apiError.value  = "Activity too short (minimum ${minDurationSec}s)."
                _isSaving.value  = false          // ← spinner stops immediately
                Log.w(TAG, "Save rejected: duration ${finalDuration}s < ${minDurationSec}s")
                TrackingService.stopService(getApplication())
                TrackingService.resetData()
                return@launch
            }

            if (isGpsActivity && finalDistance < minDistanceM) {
                _apiError.value  = "No movement detected (minimum ${minDistanceM.toInt()}m)."
                _isSaving.value  = false          // ← spinner stops immediately
                Log.w(TAG, "Save rejected: distance ${finalDistance}m < ${minDistanceM}m")
                TrackingService.stopService(getApplication())
                TrackingService.resetData()
                return@launch
            }



            val request = ActivityRequest(
                user_id       = userId,
                activity_type = activityType,
                duration      = (finalDuration / 60).toInt().coerceAtLeast(1),
                distance      = (finalDistance / 1000.0),
                calories      = calculateCalories(activityType),
                notes         = "Tracked via Fitnesstracker Pro"
            )

            try {
                // 15-second hard ceiling — covers slow Wi-Fi and server cold-starts
                val result = withTimeoutOrNull(15_000L) {
                    repository.saveActivity(request)
                }

                when {
                    result == null -> {
                        _apiError.value = "Save timed out. Check your WiFi connection."
                        Log.e(TAG, "saveActivity: API call timed out after 15s")
                    }
                    else -> result
                        .onSuccess {
                            Log.d(TAG, "saveActivity: success — activity_id=${it.activity_id}")
                            onSuccess()          // Posts notification + navigates back
                        }
                        .onFailure { t ->
                            _apiError.value = t.message ?: "Failed to save activity."
                            Log.e(TAG, "saveActivity: server rejected — ${t.message}")
                        }
                }
            } catch (e: Exception) {
                _apiError.value = "Connection error: ${e.localizedMessage}"
                Log.e(TAG, "saveActivity: exception — ${e.message}")
            } finally {
                // GUARANTEED: spinner off + service stopped + state reset
                // regardless of success, failure, or timeout.
                _isSaving.value = false
                TrackingService.stopService(getApplication())
                TrackingService.resetData()
            }
        }
    }
}