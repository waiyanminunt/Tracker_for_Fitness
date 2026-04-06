package com.example.fitnesstracker.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.ui.theme.*
import com.example.fitnesstracker.ui.viewmodel.DashboardViewModel
import com.example.fitnesstracker.ui.viewmodel.ViewModelFactory
import com.example.fitnesstracker.utils.*

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = getUserId()
        val userName = getUserName()
        val userEmail = getUserEmail()

        val repository = DataRepository(ApiClient.apiService)
        val factory = ViewModelFactory(application, repository)

        setContent {
            FitnesstrackerTheme {
                val viewModel: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    userId: Int,
    userName: String,
    userEmail: String,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = MaterialTheme.colorScheme.background

    var hasScreenTimePermission by remember { mutableStateOf(false) }
    var screenTime by remember { mutableLongStateOf(0L) }
    var isOverLimit by remember { mutableStateOf(false) }
    var waterIntake by remember { mutableIntStateOf(0) }
    var waterGoal by remember { mutableIntStateOf(2000) }

    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val totalCalories by viewModel.totalCalories.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalDuration.collectAsStateWithLifecycle()
    val totalSessions by viewModel.totalSessions.collectAsStateWithLifecycle()
    val apiError by viewModel.error.collectAsStateWithLifecycle()

    // ── Data loading: cache-aware ON_RESUME ──────────────────────────────────
    // fetchActivities() respects the ViewModel's 30-second in-memory cache.
    // Returning from sub-screens (Profile, Notifications, etc.) within that window
    // is a fast no-op — the ViewModel serves existing data instantly.
    // Only forceRefresh() bypasses the cache (called after saving a new activity).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && userId > 0) {
                viewModel.fetchActivities(userId)   // no-op if cache is fresh
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(apiError) {
        apiError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Re-read wellness stats (screen time permission + water intake) on every resume.
    // LaunchedEffect(Unit) only fires ONCE — returning from system Usage Access
    // Settings would never re-evaluate the permission. ON_RESUME fires every time.
    val wellnessLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(wellnessLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                try {
                    val screenTimeHelper  = ScreenTimeHelper(context)
                    val waterTrackerHelper = WaterTrackerHelper(context)

                    hasScreenTimePermission = screenTimeHelper.hasUsageStatsPermission()

                    if (hasScreenTimePermission) {
                        screenTime  = screenTimeHelper.getTodayScreenTime()
                        isOverLimit = screenTimeHelper.isOverScreenTimeLimit()
                    }

                    waterIntake = waterTrackerHelper.getTodayIntake()
                    waterGoal   = waterTrackerHelper.getDailyGoal()

                } catch (e: Exception) {
                    Log.e("Dashboard", "Wellness resume error: ${e.message}")
                }
            }
        }
        wellnessLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { wellnessLifecycleOwner.lifecycle.removeObserver(observer) }
    }


    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    context.startActivity(intent)
                },
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor.copy(alpha = 0.92f),
                contentColor = onSurfaceColor,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        val intent = Intent(context, AddActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Add") },
                    label = { Text("Add") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        val intent = Intent(context, StatisticsActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Stats") },
                    label = { Text("Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        val intent = Intent(context, ProfileActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        intent.putExtra("USER_NAME", userName)
                        intent.putExtra("USER_EMAIL", userEmail)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$greeting, $userName!",
                            color = onSurfaceColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Let's track your fitness",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    val notificationHelper = remember { NotificationHelper(context) }
                    val unreadCount = remember { mutableIntStateOf(0) }

                    // Re-read unread count every time Dashboard resumes
                    // (catches the case where user opened NotificationsActivity and
                    // markAllAsRead() was called, so the badge clears immediately on return)
                    val notifLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(notifLifecycleOwner) {
                        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                unreadCount.intValue = notificationHelper.getUnreadCount()
                            }
                        }
                        notifLifecycleOwner.lifecycle.addObserver(obs)
                        onDispose { notifLifecycleOwner.lifecycle.removeObserver(obs) }
                    }

                    Box {
                        IconButton(onClick = {
                            val intent = Intent(context, NotificationsActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = onSurfaceColor
                            )
                        }

                        if (unreadCount.intValue > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(Color.Red, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount.intValue > 9) "9+" else "${unreadCount.intValue}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                SummaryOverviewCard(
                    totalCalories = totalCalories,
                    totalDuration = totalDuration,
                    totalSessions = totalSessions,
                    calorieGoal = 2500
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Daily Wellness",
                    color = onSurfaceColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WellnessQuickCard(
                        title = "Hydration",
                        value = "$waterIntake ml",
                        progress = if (waterGoal > 0) waterIntake.toFloat() / waterGoal else 0f,
                        icon = Icons.Default.WaterDrop,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                val waterHelper = WaterTrackerHelper(context)
                                waterHelper.addWater(250)
                                waterIntake = waterHelper.getTodayIntake()
                            } catch (e: Exception) {
                                Log.e("Dashboard", "Error adding water: ${e.message}")
                            }
                        }
                    )
                    WellnessQuickCard(
                        title = "Screen Time",
                        // Show real usage when permitted; prompt to grant when not
                        value = if (hasScreenTimePermission)
                            "${screenTime / (1000L * 60L * 60L)}h ${(screenTime / (1000L * 60L)) % 60L}m"
                        else
                            "Tap to Grant",
                        progress = if (hasScreenTimePermission && screenTime > 0L)
                            (screenTime.toFloat() / (7f * 3600f * 1000f)).coerceIn(0f, 1f)
                        else 0f,
                        icon = Icons.Default.Smartphone,
                        color = when {
                            !hasScreenTimePermission -> Color(0xFF9E9E9E)   // grey = no permission
                            isOverLimit             -> Color(0xFFFF5722)   // orange = over limit
                            else                    -> primaryColor
                        },
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!hasScreenTimePermission) {
                                // Open system Usage Access settings so user can grant permission
                                try { ScreenTimeHelper(context).openUsageStatsSettings() }
                                catch (e: Exception) { Log.e("Dashboard", "Cannot open usage settings", e) }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activities",
                        color = onSurfaceColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        val intent = Intent(context, StatisticsActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        context.startActivity(intent)
                    }) {
                        Text("View All", color = primaryColor)
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp)
                        )
                    } else if (activities.isEmpty()) {
                        EmptyActivitiesState()
                    } else {
                        activities.take(5).forEach { activity ->
                            ActivityItem(activity)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SummaryOverviewCard(
    totalCalories: Int,
    totalDuration: Int,
    totalSessions: Int,
    calorieGoal: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .liquidShadow(elevation = 16.dp, shape = ShapeCard, color = primaryColor.copy(alpha = 0.22f))
            .fluidAnimate()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LiquidCardGradient)
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { (totalCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalCalories",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "kcal",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(start = 24.dp)
                    ) {
                        SummaryStatRow(
                            label = "Workouts",
                            value = "$totalSessions",
                            icon = Icons.Default.FitnessCenter,
                            color = Color.White
                        )
                        SummaryStatRow(
                            label = "Duration",
                            value = "${totalDuration}m",
                            icon = Icons.Default.Timer,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                        SummaryStatRow(
                            label = "Goal",
                            value = "$calorieGoal",
                            icon = Icons.Default.Flag,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatRow(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WellnessQuickCard(
    title: String,
    value: String,
    progress: Float,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .liquidShadow(elevation = 10.dp, shape = ShapeCard, color = color.copy(alpha = 0.18f))
            .fluidAnimate(),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.08f))
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
fun EmptyActivitiesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No activities yet",
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ActivityItem(activity: ActivityData) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fluidAnimate(),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (activity.activity_type) {
                "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
                "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                "Swimming" -> Icons.Default.Pool
                "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
                else -> Icons.Default.FitnessCenter
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = activity.activity_type,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.activity_type,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // FIXED: Removed invalid escaped quotes " and used proper Kotlin string format
                    text = "${activity.duration} min • ${"%.1f".format(activity.distance)} km",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${activity.calories}",
                    color = primaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "kcal",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}