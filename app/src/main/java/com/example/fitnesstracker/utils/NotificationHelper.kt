package com.example.fitnesstracker.utils

import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.network.*
import com.example.fitnesstracker.data.models.*
import com.example.fitnesstracker.utils.*
import com.example.fitnesstracker.receivers.*
import com.example.fitnesstracker.ui.activities.*

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class to manage notification storage and display.
 * All notifications are stored locally in SharedPreferences as a JSON array.
 * There is intentionally no backend sync — notifications are device-local logs.
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

    /** Returns the full ordered list of stored notifications, newest first. */
    fun getNotifications(): List<NotificationItem> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Prepends a new notification to the list, capping at [MAX_NOTIFICATIONS]. */
    fun addNotification(title: String, message: String, type: String) {
        val notifications = getNotifications().toMutableList()

        val notification = NotificationItem(
            id        = System.currentTimeMillis(),
            title     = title,
            message   = message,
            type      = type,
            timestamp = System.currentTimeMillis(),
            isRead    = false
        )

        notifications.add(0, notification)

        val capped = if (notifications.size > MAX_NOTIFICATIONS)
            notifications.take(MAX_NOTIFICATIONS)
        else
            notifications

        saveNotifications(capped)
    }

    /** Persists the given list to SharedPreferences. */
    private fun saveNotifications(notifications: List<NotificationItem>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    /** Marks a single notification as read by its [notificationId]. */
    fun markAsRead(notificationId: Long) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveNotifications(notifications)
        }
    }

    /** Marks all notifications as read. Called when the screen opens. */
    fun markAllAsRead() {
        val notifications = getNotifications().map { it.copy(isRead = true) }
        saveNotifications(notifications)
    }

    /** Removes all notifications. Triggered by the "Clear All" button. */
    fun clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    /**
     * Deletes a single notification by its unique [id].
     * Used by the swipe-to-delete gesture in [NotificationsScreen].
     * Optimistic UI: The composable removes the item from its in-memory list first,
     * then this method persists the change.
     */
    fun deleteById(id: Long) {
        val updated = getNotifications().filter { it.id != id }
        saveNotifications(updated)
    }

    /** Returns the count of unread notifications (used for badge indicators). */
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }

    // ── Convenience factory methods ────────────────────────────────────────────

    fun addWaterReminder() {
        addNotification(
            title   = "💧 Time to Drink Water!",
            message = "Stay hydrated! Drink a glass of water now.",
            type    = "water"
        )
    }

    fun addScreenTimeWarning() {
        addNotification(
            title   = "⚠️ High Screen Time!",
            message = "You've been using your phone for over 7 hours. Take a break!",
            type    = "screen_time"
        )
    }

    fun addWorkoutReminder() {
        addNotification(
            title   = "🏋️ Time to Workout!",
            message = "Don't forget your daily workout session!",
            type    = "workout"
        )
    }

    fun addAchievement(achievement: String) {
        addNotification(
            title   = "🏆 Achievement Unlocked!",
            message = achievement,
            type    = "achievement"
        )
    }
}

/** Data class for a single notification entry. */
data class NotificationItem(
    val id:        Long,
    val title:     String,
    val message:   String,
    val type:      String,
    val timestamp: Long,
    val isRead:    Boolean
) {
    /** Returns a human-readable relative timestamp (e.g., "5m ago", "2h ago"). */
    fun getFormattedTime(): String {
        val now  = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = diff / (1000 * 60)
        val hours   = diff / (1000 * 60 * 60)
        val days    = diff / (1000 * 60 * 60 * 24)

        return when {
            minutes < 1  -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours   < 24 -> "${hours}h ago"
            days    < 7  -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}