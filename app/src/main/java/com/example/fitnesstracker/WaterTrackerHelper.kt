package com.example.fitnesstracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import java.util.Calendar

/**
 * Helper class to track water intake and manage reminders
 */
class WaterTrackerHelper(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
    }

    companion object {
        const val CHANNEL_ID = "water_reminder"
        const val DAILY_GOAL_ML = 2500
        const val REMINDER_INTERVAL_HOURS = 2
        const val NOTIFICATION_ID = 1001
    }

    /**
     * Get today's water intake in milliliters
     */
    fun getTodayIntake(): Int {
        val todayKey = getTodayKey()
        return prefs.getInt(todayKey, 0)
    }

    /**
     * Add water to today's intake
     */
    fun addWater(amountMl: Int) {
        val todayKey = getTodayKey()
        val current = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, current + amountMl).apply()
    }

    /**
     * Reset today's water intake
     */
    fun resetTodayIntake() {
        val todayKey = getTodayKey()
        prefs.edit().putInt(todayKey, 0).apply()
    }

    /**
     * Get progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        val intake = getTodayIntake()
        return ((intake.toFloat() / DAILY_GOAL_ML) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Get remaining water to drink
     */
    fun getRemaining(): Int {
        val intake = getTodayIntake()
        return (DAILY_GOAL_ML - intake).coerceAtLeast(0)
    }

    /**
     * Check if daily goal is reached
     */
    fun isGoalReached(): Boolean {
        return getTodayIntake() >= DAILY_GOAL_ML
    }

    /**
     * Set daily goal
     */
    fun setDailyGoal(goalMl: Int) {
        prefs.edit().putInt("daily_goal", goalMl).apply()
    }

    /**
     * Get daily goal
     */
    fun getDailyGoal(): Int {
        return prefs.getInt("daily_goal", DAILY_GOAL_ML)
    }

    /**
     * Get today's key for SharedPreferences
     */
    private fun getTodayKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "water_$year$month$day"
    }

    /**
     * Create notification channel for water reminders
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to remind you to drink water"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule water reminder notifications
     */
    fun scheduleReminders() {
        createNotificationChannel()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminderHours = listOf(8, 10, 12, 14, 16, 18, 20, 22)

        for (hour in reminderHours) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                hour,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Cancel all water reminders
     */
    fun cancelReminders() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderHours = listOf(8, 10, 12, 14, 16, 18, 20, 22)

        for (hour in reminderHours) {
            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                hour,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}