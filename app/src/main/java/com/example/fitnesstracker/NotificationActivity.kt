package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// INHERITANCE EXAMPLE: NotificationsActivity extends BaseActivity
// ============================================
// This class DEMONSTRATES INHERITANCE by:
// 1. Extending BaseActivity (inherits helper methods)
// 2. Using inherited colors and methods
// ============================================

class NotificationsActivity : BaseActivity() {

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationHelper = NotificationHelper(this)

        setContent {
            NotificationsScreen(
                onBack = { finish() },
                notificationHelper = notificationHelper
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    notificationHelper: NotificationHelper
) {
    var notifications by remember { mutableStateOf(notificationHelper.getNotifications()) }
    var unreadCount by remember { mutableStateOf(notificationHelper.getUnreadCount()) }

    val darkPurple = Color(0xFF1A0A2E)
    val purple = Color(0xFF6B4C9A)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)
    val blueColor = Color(0xFF2196F3)
    val orangeColor = Color(0xFFFF9800)
    val greenColor = Color(0xFF4CAF50)
    val redColor = Color(0xFFE53935)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkPurple)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Notifications",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (unreadCount > 0) {
                TextButton(onClick = {
                    notificationHelper.markAllAsRead()
                    notifications = notificationHelper.getNotifications()
                    unreadCount = 0
                }) {
                    Text(
                        text = "Mark all read",
                        color = lightPurple,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Unread count badge
        if (unreadCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = purple.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = lightPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "You have $unreadCount unread notification${if (unreadCount > 1) "s" else ""}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Notifications list
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Your notifications will appear here",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            notificationHelper.markAsRead(notification.id)
                            notifications = notificationHelper.getNotifications()
                            unreadCount = notificationHelper.getUnreadCount()
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Clear all button
            TextButton(
                onClick = {
                    notificationHelper.clearAll()
                    notifications = emptyList()
                    unreadCount = 0
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = redColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clear All Notifications",
                    color = redColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val cardBg = Color(0xFF2D1B4E)
    val blueColor = Color(0xFF2196F3)
    val orangeColor = Color(0xFFFF9800)
    val greenColor = Color(0xFF4CAF50)
    val redColor = Color(0xFFE53935)
    val yellowColor = Color(0xFFFFC107)

    val (icon, color) = when (notification.type) {
        "water" -> Icons.Default.WaterDrop to blueColor
        "screen_time" -> Icons.Default.Timer to redColor
        "workout" -> Icons.Default.FitnessCenter to orangeColor
        "achievement" -> Icons.Default.EmojiEvents to yellowColor
        else -> Icons.Default.Notifications to greenColor
    }

    val alpha = if (notification.isRead) 0.5f else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = notification.type,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        color = Color.White.copy(alpha = alpha),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Red, RoundedCornerShape(4.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    color = Color.Gray.copy(alpha = alpha),
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.getFormattedTime(),
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}