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
import com.example.fitnesstracker.utils.ScreenTimeHelper

/**
 * ScreenTimeReceiver — fired hourly by AlarmManager (scheduled in FitnessApp.onCreate).
 *
 * Checks whether the user's total device screen time today has exceeded the 7-hour
 * warning threshold. Posts a system notification if it has, and records the event
 * in the in-app NotificationHelper history so it appears in the Notifications screen.
 *
 * Guard: if PACKAGE_USAGE_STATS permission is not granted, this receiver is a no-op.
 */
class ScreenTimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val screenTimeHelper = ScreenTimeHelper(context)

        // Do nothing if the user has not granted Usage Access permission
        if (!screenTimeHelper.hasUsageStatsPermission()) return

        if (screenTimeHelper.isOverScreenTimeLimit()) {
            // Record in the in-app notification history (Notifications screen)
            val notificationHelper = NotificationHelper(context)
            notificationHelper.addScreenTimeWarning()

            // Open LoginActivity (lands on Dashboard if already logged in via SharedPrefs)
            val notificationIntent: Intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ High Screen Time!")
                .setContentText("You've been using your phone for over 7 hours. Take a break!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.RED)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Warning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings when screen time is too high"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.RED
            }
            val notificationManager =
                context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID      = "screen_time_warning"
        const val NOTIFICATION_ID = 1002
    }
}