package com.example.fitnesstracker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class to manage notification storage and display
 */
class NotificationHelper(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    companion object {
        const val KEY_NOTIFICATIONS = "notification_list"
        const val MAX_NOTIFICATIONS = 50
    }

    /**
     * Get all notifications
     */
    fun getNotifications(): List<NotificationItem> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a new notification
     */
    fun addNotification(title: String, message: String, type: String) {
        val notifications = getNotifications().toMutableList()

        val notification = NotificationItem(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        notifications.add(0, notification)

        // Keep only last MAX_NOTIFICATIONS
        if (notifications.size > MAX_NOTIFICATIONS) {
            val trimmed = notifications.take(MAX_NOTIFICATIONS)
            saveNotifications(trimmed)
        } else {
            saveNotifications(notifications)
        }
    }

    /**
     * Save notifications to SharedPreferences
     */
    private fun saveNotifications(notifications: List<NotificationItem>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: Long) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveNotifications(notifications)
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        val notifications = getNotifications().map { it.copy(isRead = true) }
        saveNotifications(notifications)
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    /**
     * Get unread count
     */
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }

    /**
     * Add water reminder notification
     */
    fun addWaterReminder() {
        addNotification(
            title = "💧 Time to Drink Water!",
            message = "Stay hydrated! Drink a glass of water now.",
            type = "water"
        )
    }

    /**
     * Add screen time warning notification
     */
    fun addScreenTimeWarning() {
        addNotification(
            title = "⚠️ High Screen Time!",
            message = "You've been using your phone for over 7 hours. Take a break!",
            type = "screen_time"
        )
    }

    /**
     * Add workout reminder notification
     */
    fun addWorkoutReminder() {
        addNotification(
            title = "🏋️ Time to Workout!",
            message = "Don't forget your daily workout session!",
            type = "workout"
        )
    }

    /**
     * Add achievement notification
     */
    fun addAchievement(achievement: String) {
        addNotification(
            title = "🏆 Achievement Unlocked!",
            message = achievement,
            type = "achievement"
        )
    }
}

/**
 * Data class for notification item
 */
data class NotificationItem(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean
) {
    /**
     * Get formatted time
     */
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}