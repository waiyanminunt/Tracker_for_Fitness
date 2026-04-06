package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.utils.BaseActivity
import com.example.fitnesstracker.utils.NotificationHelper
import com.example.fitnesstracker.utils.NotificationItem
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// ============================================
// INHERITANCE: NotificationsActivity extends BaseActivity
// ============================================
class NotificationsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnesstrackerTheme {
                NotificationsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context          = LocalContext.current
    val lifecycleOwner   = LocalLifecycleOwner.current
    val notificationHelper = remember { NotificationHelper(context) }

    // ── Reactive state ────────────────────────────────────────────────────────
    // MutableStateList gives Compose fine-grained recomposition on item removal.
    var notifications by remember {
        mutableStateOf(notificationHelper.getNotifications().reversed())
    }

    // Re-read from SharedPreferences every time the screen resumes (ON_RESUME).
    // This ensures newly added notifications (from tracking/add screens) appear immediately.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationHelper.markAllAsRead()
                notifications = notificationHelper.getNotifications().reversed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Clear All confirmation dialog ─────────────────────────────────────────
    var showClearAllDialog by remember { mutableStateOf(false) }
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            shape            = RoundedCornerShape(16.dp),
            title  = { Text("Clear All Notifications", fontWeight = FontWeight.Bold) },
            text   = { Text("This will permanently delete all notifications. This action cannot be undone.") },
            confirmButton  = {
                Button(
                    onClick = {
                        notificationHelper.clearAll()
                        notifications = emptyList()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton  = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            NotificationsTopBar(
                itemCount      = notifications.size,
                onBack         = onBack,
                onClearAll     = { showClearAllDialog = true }
            )
        },
        containerColor = backgroundColor
    ) { padding ->

        if (notifications.isEmpty()) {
            // ── Empty state ───────────────────────────────────────────────────
            Box(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint               = Color.Gray,
                        modifier           = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text      = "No notifications yet",
                        color     = MaterialTheme.colorScheme.onBackground,
                        fontSize  = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text     = "Save an activity to see your history here",
                        color    = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // ── Notification list with swipe-to-delete ────────────────────────
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding      = PaddingValues(vertical = 12.dp)
            ) {
                items(
                    items = notifications,
                    key   = { it.id }   // stable key — required for animateItem correctness
                ) { notification ->
                    // animateItem() MUST be called here inside the items{} lambda
                    // because it is a LazyItemScope extension — not valid inside
                    // a plain @Composable function.
                    SwipeToDeleteNotificationCard(
                        notification = notification,
                        modifier     = Modifier.animateItem(),
                        onDelete     = {
                            // Optimistic UI: remove from memory first → instant visual feedback
                            notifications = notifications.filter { it.id != notification.id }
                            // Then persist deletion to SharedPreferences
                            notificationHelper.deleteById(notification.id)
                        }
                    )
                }
            }
        }
    }
}

// ── Top bar with dynamic "Clear All" action ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsTopBar(
    itemCount:  Int,
    onBack:     () -> Unit,
    onClearAll: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text       = "Notifications",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
                if (itemCount > 0) {
                    Text(
                        text     = "$itemCount items",
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (itemCount > 0) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector        = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ── Swipe-to-delete wrapper ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteNotificationCard(
    notification: NotificationItem,
    onDelete:     () -> Unit,
    modifier:     Modifier = Modifier   // receives Modifier.animateItem() from items{} call site
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Only honour swipe from end (right→left) to trigger delete
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.40f }
    )

    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,   // left-swipe only
        enableDismissFromEndToStart = true,
        backgroundContent           = { SwipeDeleteBackground(dismissState) },
        content                     = { NotificationItemCard(notification) },
        modifier                    = modifier  // animateItem() applied from LazyItemScope above
    )
}

/** Red background with a trash icon revealed on swipe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteBackground(dismissState: SwipeToDismissBoxState) {
    val isActive   = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val bgColor    by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
        label       = "swipe_bg"
    )
    val iconScale  by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1.0f,
        label       = "icon_scale"
    )

    Box(
        modifier        = Modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(iconScale)
        ) {
            Icon(
                imageVector        = Icons.Default.Delete,
                contentDescription = "Delete",
                tint               = Color.White,
                modifier           = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = "Delete",
                color    = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Notification card ──────────────────────────────────────────────────────────

@Composable
fun NotificationItemCard(notification: NotificationItem) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pick icon + tint by notification type
    val (icon, iconTint) = notificationIconFor(notification.type, primaryColor)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (notification.isRead) surfaceColor
                             else surfaceColor.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 2.dp)
    ) {
        Row(
            modifier          = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon badge
            Box(
                modifier        = Modifier
                    .size(42.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment  = Alignment.CenterVertically
                ) {
                    Text(
                        text       = notification.title,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        // Unread dot indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(primaryColor, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = notification.message,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text     = notification.getFormattedTime(),
                    color    = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Maps notification type string to a Material icon + accent colour. */
@Composable
private fun notificationIconFor(type: String, fallback: Color): Pair<ImageVector, Color> {
    return when (type) {
        "workout"     -> Icons.Default.FitnessCenter  to Color(0xFF00BCD4)
        "water"       -> Icons.Default.WaterDrop      to Color(0xFF2196F3)
        "screen_time" -> Icons.Default.Timer          to Color(0xFFFF9800)
        "achievement" -> Icons.Default.EmojiEvents    to Color(0xFFFFC107)
        else          -> Icons.Default.Notifications  to fallback
    }
}