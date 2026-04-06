package com.example.fitnesstracker

import android.app.Application
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.receivers.ScreenTimeReceiver
import com.example.fitnesstracker.utils.ThemeManager
import com.example.fitnesstracker.utils.WaterTrackerHelper
import java.util.concurrent.TimeUnit

/**
 * Custom Application class — executes once when the app process starts,
 * before any Activity is created.
 *
 * Responsibilities:
 *   1. Restore dark-mode preference into ThemeManager (zero flicker on launch).
 *   2. Initialize the OkHttp 10 MB disk response cache.
 *   3. Schedule water reminder alarms (8 fixed times per day).
 *      Runs every launch so reminders survive app updates and reinstalls.
 *   4. Schedule hourly ScreenTimeReceiver alarm for background screen-time warnings.
 *      The receiver fires only when screen time > 7 h and permission is granted.
 */
class FitnessApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── 1. Restore dark-mode preference ───────────────────────────────────
        val prefs = getSharedPreferences("FitnessTrackerPrefs", MODE_PRIVATE)
        ThemeManager.isDarkMode.value = prefs.getBoolean("isDarkMode", false)

        // ── 2. OkHttp disk cache ───────────────────────────────────────────────
        ApiClient.initCache(cacheDir)

        // ── 3. Water reminder alarms ───────────────────────────────────────────
        // scheduleReminders() is idempotent — AlarmManager uses FLAG_UPDATE_CURRENT
        // so re-scheduling on every launch is safe and ensures reminders survive
        // app updates and reinstalls (WaterReminderReceiver.BOOT_COMPLETED handles reboots).
        try {
            WaterTrackerHelper(this).scheduleReminders()
            Log.d(TAG, "Water reminders scheduled")
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule water reminders: ${e.message}")
        }

        // ── 4. Hourly ScreenTimeReceiver alarm ────────────────────────────────
        // Fires ScreenTimeReceiver every 60 minutes so it can check whether
        // total screen time today has exceeded the 7-hour warning threshold.
        // The receiver itself guards: if permission not granted → no-op.
        scheduleHourlyScreenTimeCheck()
    }

    /**
     * Schedules ScreenTimeReceiver to fire every 60 minutes via AlarmManager.
     * Uses setInexactRepeating so the OS can batch alarms for battery efficiency.
     * Safe to call multiple times — AlarmManager replaces any existing alarm
     * with the same PendingIntent (FLAG_UPDATE_CURRENT).
     */
    private fun scheduleHourlyScreenTimeCheck() {
        try {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val intent       = Intent(this, ScreenTimeReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                SCREEN_TIME_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(60),
                AlarmManager.INTERVAL_HOUR,
                pendingIntent
            )
            Log.d(TAG, "ScreenTimeReceiver hourly alarm scheduled")
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule screen time alarm: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FitnessApp"
        private const val SCREEN_TIME_ALARM_REQUEST_CODE = 9001
    }
}
