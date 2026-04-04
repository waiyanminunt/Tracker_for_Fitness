package com.example.fitnesstracker.receivers
import com.example.fitnesstracker.R

import com.example.fitnesstracker.data.network.*
import com.example.fitnesstracker.data.models.*
import com.example.fitnesstracker.utils.*
import com.example.fitnesstracker.receivers.*
import com.example.fitnesstracker.ui.activities.*

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

class ScreenTimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val screenTimeHelper = ScreenTimeHelper(context)

        if (screenTimeHelper.hasUsageStatsPermission() && screenTimeHelper.isOverScreenTimeLimit()) {
            // Save to notification history
            val notificationHelper = NotificationHelper(context)
            notificationHelper.addScreenTimeWarning()

            val notificationIntent = Intent(context, LoginActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, "screen_time_warning")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ High Screen Time!")
                .setContentText("You've been using your phone for over 7 hours. Take a break!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(Color.RED)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1002, notification)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "screen_time_warning",
                "Screen Time Warning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings when screen time is too high"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.RED
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}