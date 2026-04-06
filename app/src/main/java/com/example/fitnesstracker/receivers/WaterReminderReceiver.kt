package com.example.fitnesstracker.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.ui.activities.LoginActivity
import com.example.fitnesstracker.utils.NotificationHelper

/**
 * WaterReminderReceiver — fired by AlarmManager at preset hourly intervals
 * (08:00, 10:00, 12:00, 14:00, 16:00, 18:00, 20:00, 22:00) as scheduled
 * by WaterTrackerHelper.scheduleReminders().
 *
 * Also fired on BOOT_COMPLETED (registered in AndroidManifest) so reminders
 * survive device reboots automatically.
 *
 * On each fire: posts a system notification reminding the user to drink water,
 * and records the reminder in the in-app NotificationHelper history so it
 * appears on the Notifications screen.
 */
class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        // Record in the in-app notification history (Notifications screen)
        val notificationHelper = NotificationHelper(context)
        notificationHelper.addWaterReminder()

        // Tap-through opens the app (LoginActivity routes to Dashboard if logged in)
        val notificationIntent: Intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💧 Time to Drink Water!")
            .setContentText("Stay hydrated! Tap to log your water intake.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.BLUE)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to remind you to drink water"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.BLUE
            }
            val notificationManager =
                context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID      = "water_reminder"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE    = 2001
    }
}