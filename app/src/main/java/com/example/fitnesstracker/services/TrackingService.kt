/**
 * FitnessTracker Pro — Background Tracking Service
 *
 * Runs as a foreground [Service] to track active fitness sessions.
 * It is the **single source of truth** for all live tracking data —
 * distance, duration, speed, and GPS coordinates.
 *
 * Architecture (SOLID — Single Responsibility Principle):
 *   Each private function does exactly one thing:
 *     [startTimer]           → manages the 1-second tick coroutine
 *     [startLocationUpdates] → registers the GPS listener
 *     [processLocationBatch] → validates and accumulates location points
 *     [computeSpeedKmh]      → derives speed from hardware or coordinate delta
 *     [buildNotification]    → constructs the persistent status notification
 *     [updateNotification]   → refreshes the notification with current state
 *
 * Activity Isolation:
 *   The [_activeActivityType] lock ensures that once a session of type
 *   "Hiking" starts, no other activity type can hijack the service until
 *   the session is explicitly stopped via [ACTION_STOP].
 *
 * GPS Data Quality:
 *   Points with accuracy > [MAX_ACCEPTABLE_ACCURACY_METERS] are discarded.
 *   Movements smaller than [MIN_MOVEMENT_THRESHOLD_METERS] are ignored to
 *   suppress GPS jitter (false distance accumulation while standing still).
 */
package com.example.fitnesstracker.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.R
import com.example.fitnesstracker.ui.activities.TrackingActivity
import com.example.fitnesstracker.ui.viewmodel.TrackingState
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TrackingService : Service() {

    // ── Instance-level resources ───────────────────────────────────────────────

    /** Coroutine scope tied to the service lifecycle. Cancelled in [onDestroy]. */
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    /** Reference to the running timer coroutine, so it can be cancelled on pause. */
    private var timerJob: Job? = null

    /** Google Play Services GPS client. Re-initialised on resume to prevent dropped updates. */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ── Companion: shared (observable) state & intent helpers ──────────────────

    companion object {
        private const val TAG = "TrackingService"
        private const val NOTIFICATION_CHANNEL_ID = "fitness_tracking_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1

        // Intent actions — used to command the service from the ViewModel
        const val ACTION_START  = "ACTION_START"
        const val ACTION_PAUSE  = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP   = "ACTION_STOP"
        const val EXTRA_ACTIVITY_TYPE = "EXTRA_ACTIVITY_TYPE"

        // GPS quality thresholds
        /** Locations with accuracy worse than this (in metres) are ignored. */
        private const val MAX_ACCEPTABLE_ACCURACY_METERS = 100f

        /**
         * Minimum displacement between two consecutive GPS points (in metres)
         * before counting it as real movement. Prevents jitter-based
         * false distance accumulation while stationary.
         */
        private const val MIN_MOVEMENT_THRESHOLD_METERS = 0.5f

        /** Location update interval requested from FusedLocationProvider (ms). */
        private const val LOCATION_UPDATE_INTERVAL_MS = 2000L

        /** Fastest acceptable location update interval (ms). */
        private const val LOCATION_FASTEST_INTERVAL_MS = 1000L

        // ── Reactive state flows (observed by TrackingViewModel) ───────────────

        private val _trackingState = MutableStateFlow(TrackingState.IDLE)
        /** Current phase of the tracking session: IDLE, RUNNING, PAUSED, or STOPPED. */
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        private val _activeActivityType = MutableStateFlow<String?>(null)
        /**
         * The activity type currently locked to this service instance
         * (e.g. "Hiking"). Null when no session is active. Once set,
         * any attempt to start a different type is rejected by [TrackingViewModel].
         */
        val activeActivityType: StateFlow<String?> = _activeActivityType.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        /** Total elapsed time for the current session, in seconds. Pausing stops the counter. */
        val durationSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

        private val _accumulatedDistanceMeters = MutableStateFlow(0f)
        /** Running total of validated movement for this session, in metres. */
        val distanceMeters: StateFlow<Float> = _accumulatedDistanceMeters.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0f)
        /**
         * Most recently computed speed in km/h.
         * Derived from [Location.speed] when available; otherwise calculated
         * from the delta between the last two validated GPS points.
         */
        val currentSpeed: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

        private val _recordedLocationPoints = MutableStateFlow<List<Location>>(emptyList())
        /** Ordered list of all accepted GPS points for map rendering. NOT cleared on pause. */
        val locationPoints: StateFlow<List<Location>> = _recordedLocationPoints.asStateFlow()

        // ── State management helpers ───────────────────────────────────────────

        /**
         * Resets all tracking state to zero.
         * Must be called before starting a fresh session (not on resume).
         */
        fun resetData() {
            Log.d(TAG, "resetData: Clearing all session state")
            _elapsedSeconds.value = 0L
            _accumulatedDistanceMeters.value = 0f
            _currentSpeedKmh.value = 0f
            _recordedLocationPoints.value = emptyList()
            _activeActivityType.value = null
            _trackingState.value = TrackingState.IDLE
        }

        // ── Intent factory helpers (keep callers free of Intent construction) ──

        /** Builds and dispatches the [ACTION_START] intent. */
        fun startService(context: Context, activityType: String) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACTIVITY_TYPE, activityType)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Dispatches the [ACTION_PAUSE] intent. */
        fun pauseService(context: Context) {
            context.startService(Intent(context, TrackingService::class.java).apply {
                action = ACTION_PAUSE
            })
        }

        /** Dispatches the [ACTION_RESUME] intent. */
        fun resumeService(context: Context) {
            context.startService(Intent(context, TrackingService::class.java).apply {
                action = ACTION_RESUME
            })
        }

        /** Dispatches the [ACTION_STOP] intent. */
        fun stopService(context: Context) {
            context.startService(Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    // ── Service lifecycle ──────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Entry point for all intent-based commands sent by [TrackingViewModel].
     * Routes intents to the appropriate private handler.
     * Returns [START_STICKY] so Android restarts the service if it is killed.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")
        when (action) {
            ACTION_START  -> startTracking(intent.getStringExtra(EXTRA_ACTIVITY_TYPE))
            ACTION_PAUSE  -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP   -> stopTracking()
            else          -> Log.w(TAG, "onStartCommand: unknown action — $action")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Releasing resources")
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    // ── Session state machine ──────────────────────────────────────────────────

    /**
     * Starts a new tracking session locked to [activityType].
     * Guard: if a session is already [TrackingState.RUNNING], this is a no-op
     * to prevent duplicate timers and location listeners.
     */
    private fun startTracking(activityType: String?) {
        if (_trackingState.value == TrackingState.RUNNING) {
            Log.w(TAG, "startTracking: already running — ignoring duplicate start")
            return
        }
        Log.d(TAG, "startTracking: beginning session for $activityType")
        _activeActivityType.value = activityType
        _trackingState.value = TrackingState.RUNNING
        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification())
        startTimer()
        startLocationUpdates()
    }

    /**
     * Suspends the active session.
     * Stops the timer tick and removes GPS updates to conserve battery
     * and prevent "teleportation" distance errors after a long pause.
     */
    private fun pauseTracking() {
        if (_trackingState.value != TrackingState.RUNNING) return
        Log.d(TAG, "pauseTracking: suspending session")
        _trackingState.value = TrackingState.PAUSED
        timerJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        updateNotification(statusOverride = "Paused")
    }

    /**
     * Resumes a paused session.
     * Re-initialises [fusedLocationClient] to avoid dropped updates that can
     * occur when the client is idle for an extended period.
     */
    private fun resumeTracking() {
        if (_trackingState.value != TrackingState.PAUSED) return
        Log.d(TAG, "resumeTracking: continuing session")
        _trackingState.value = TrackingState.RUNNING
        // Re-init to prevent GPS dropout after extended pause
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startTimer()
        startLocationUpdates()
    }

    /**
     * Terminates the tracking session and releases the foreground service.
     * Called after the ViewModel has persisted the activity to the backend.
     */
    private fun stopTracking() {
        Log.d(TAG, "stopTracking: ending session")
        _trackingState.value = TrackingState.STOPPED
        timerJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Timer ──────────────────────────────────────────────────────────────────

    /**
     * Launches a coroutine that increments [_elapsedSeconds] every second.
     * Cancels any existing timer before starting to prevent double-counting.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_trackingState.value == TrackingState.RUNNING) {
                delay(1_000)
                _elapsedSeconds.value++
                updateNotification()
            }
        }
    }

    // ── GPS Location Updates ───────────────────────────────────────────────────

    /**
     * Registers [locationCallback] with the fused location provider.
     * Requests high-accuracy mode (GPS + network) at the configured intervals.
     */
    private fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: registering GPS listener")
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "startLocationUpdates: location permission not granted — ${e.message}")
        }
    }

    /**
     * Receives batches of GPS fixes from the fused provider.
     * Delegates actual processing to [processLocationBatch].
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            val incomingLocations = result.locations
            if (incomingLocations.isEmpty()) return
            Log.d(TAG, "onLocationResult: received ${incomingLocations.size} location(s)")
            processLocationBatch(incomingLocations)
        }
    }

    /**
     * Validates, filters, and accumulates a batch of [Location] fixes.
     *
     * For each point:
     *  1. Discards points with accuracy > [MAX_ACCEPTABLE_ACCURACY_METERS] (GPS jitter guard).
     *  2. Calculates displacement from the previous accepted point.
     *  3. Discards displacement < [MIN_MOVEMENT_THRESHOLD_METERS] (stationary noise guard).
     *  4. Accumulates distance and derives current speed via [computeSpeedKmh].
     *  5. Appends the point to [_recordedLocationPoints] for map rendering.
     */
    private fun processLocationBatch(locations: List<Location>) {
        val currentPoints = _recordedLocationPoints.value.toMutableList()

        locations.forEach { newLocation ->
            // Guard 1: Accuracy filter
            if (newLocation.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) {
                Log.d(TAG, "processLocationBatch: discarded — accuracy ${newLocation.accuracy}m exceeds ${MAX_ACCEPTABLE_ACCURACY_METERS}m threshold")
                return@forEach
            }

            val previousPoint = currentPoints.lastOrNull()

            if (previousPoint != null) {
                val displacementMeters = previousPoint.distanceTo(newLocation)

                // Guard 2: Minimum movement filter
                if (displacementMeters > MIN_MOVEMENT_THRESHOLD_METERS) {
                    _accumulatedDistanceMeters.value += displacementMeters
                    _currentSpeedKmh.value = computeSpeedKmh(newLocation, previousPoint, displacementMeters)

                    Log.d(TAG, "processLocationBatch: +%.2fm displacement | total=%.2fm | speed=%.1f km/h"
                        .format(displacementMeters, _accumulatedDistanceMeters.value, _currentSpeedKmh.value))
                }
            }

            currentPoints.add(newLocation)
        }

        _recordedLocationPoints.value = currentPoints
    }

    /**
     * Derives the current speed in km/h from a single GPS fix.
     *
     * Strategy:
     *   - Primary: Use [Location.speed] if the hardware reports it (most accurate).
     *   - Fallback: Calculate from displacement ÷ time delta between the
     *               current and previous point (for devices without a speed sensor).
     *
     * @param currentLocation  The newly received GPS fix.
     * @param previousLocation The last accepted GPS point.
     * @param displacementMeters Pre-computed distance between the two points.
     * @return Speed in km/h, guaranteed ≥ 0.
     */
    private fun computeSpeedKmh(
        currentLocation: Location,
        previousLocation: Location,
        displacementMeters: Float
    ): Float {
        return if (currentLocation.hasSpeed() && currentLocation.speed > 0) {
            // Hardware speed (m/s) → km/h
            currentLocation.speed * 3.6f
        } else {
            // Manual fallback: distance (m) / time (s) → m/s → km/h
            val elapsedSeconds = (currentLocation.time - previousLocation.time) / 1_000.0
            if (elapsedSeconds > 0) {
                (displacementMeters / elapsedSeconds * 3.6).toFloat()
            } else {
                0f
            }
        }
    }

    // ── Notification ───────────────────────────────────────────────────────────

    /**
     * Builds the initial persistent notification shown when the foreground
     * service starts. The notification channel is created here for API 26+.
     */
    private fun buildNotification(): Notification {
        ensureNotificationChannel()
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, TrackingActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking Active — ${_activeActivityType.value ?: "Activity"}")
            .setContentText("Duration: 00:00")
            .setSmallIcon(R.drawable.heart_pulse)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Refreshes the persistent notification with the current elapsed time.
     * @param statusOverride When provided (e.g. "Paused"), replaces the time display.
     */
    private fun updateNotification(statusOverride: String? = null) {
        val formattedTime = formatElapsedTime(_elapsedSeconds.value)
        val contentText = statusOverride ?: "Duration: $formattedTime"

        val updatedNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking Active — ${_activeActivityType.value ?: "Activity"}")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.heart_pulse)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, updatedNotification)
    }

    /** Creates the notification channel required on Android 8.0 (API 26) and above. */
    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of an active fitness tracking session"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Formats a raw elapsed-seconds value into a human-readable "MM:SS" or "HH:MM:SS" string.
     * @param totalSeconds Total elapsed time in seconds.
     * @return Formatted string, e.g. "05:32" or "1:12:45".
     */
    private fun formatElapsedTime(totalSeconds: Long): String {
        val hours   = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
