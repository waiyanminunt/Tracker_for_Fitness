package com.example.fitnesstracker

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/**
 * Helper class to track screen time usage
 * Uses UsageStatsManager to get app usage statistics
 */
class ScreenTimeHelper(private val context: Context) {

    private var usageStatsManager: UsageStatsManager? = null

    private fun getUsageStatsManager(): UsageStatsManager? {
        if (usageStatsManager == null) {
            usageStatsManager = context.getSystemService("usagestats") as? UsageStatsManager
        }
        return usageStatsManager
    }

    /**
     * Check if the app has permission to access usage stats
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Open Usage Stats settings for user to grant permission
     */
    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Get total screen time for today in milliseconds
     */
    fun getTodayScreenTime(): Long {
        if (!hasUsageStatsPermission()) return 0L

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStatsList: List<UsageStats>? = getUsageStatsManager()?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStatsList.isNullOrEmpty()) return 0L

        var totalTime = 0L
        for (stats in usageStatsList) {
            if (stats.totalTimeInForeground > 0) {
                totalTime += stats.totalTimeInForeground
            }
        }
        return totalTime
    }

    /**
     * Get number of phone pickups (app launches) for today
     */
    fun getTodayPickups(): Int {
        if (!hasUsageStatsPermission()) return 0

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStatsList: List<UsageStats>? = getUsageStatsManager()?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStatsList.isNullOrEmpty()) return 0

        // Count unique app launches as pickups
        return usageStatsList.count { it.totalTimeInForeground > 5000 }
    }

    /**
     * Get average session duration in minutes
     */
    fun getAverageSessionDuration(): Int {
        val totalScreenTime = getTodayScreenTime()
        val pickups = getTodayPickups()

        if (pickups == 0) return 0

        return ((totalScreenTime / pickups) / 60000).toInt()
    }

    /**
     * Get longest continuous use session in minutes
     */
    fun getLongestSession(): Int {
        if (!hasUsageStatsPermission()) return 0

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStatsList: List<UsageStats>? = getUsageStatsManager()?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStatsList.isNullOrEmpty()) return 0

        val maxTime = usageStatsList.maxOfOrNull { it.totalTimeInForeground } ?: 0L
        return (maxTime / 60000).toInt()
    }

    /**
     * Format screen time for display (e.g., "2h 30m")
     */
    fun formatScreenTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Check if screen time exceeds warning threshold (7 hours)
     */
    fun isOverScreenTimeLimit(): Boolean {
        val screenTime = getTodayScreenTime()
        val sevenHoursInMillis = 7 * 60 * 60 * 1000L
        return screenTime > sevenHoursInMillis
    }
}