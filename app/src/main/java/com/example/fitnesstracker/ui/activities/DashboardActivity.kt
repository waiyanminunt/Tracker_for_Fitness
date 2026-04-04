package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivitiesResponse
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.utils.BaseActivity
import com.example.fitnesstracker.utils.ScreenTimeHelper
import com.example.fitnesstracker.utils.WaterTrackerHelper
import com.example.fitnesstracker.utils.NotificationHelper
import com.example.fitnesstracker.utils.StatBox
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ============================================
// INHERITANCE EXAMPLE 3: DashboardActivity extends BaseActivity
// ============================================
// This class DEMONSTRATES INHERITANCE by:
// 1. Extending BaseActivity (inherits helper methods)
// 2. Using inherited methods: getUserId(), getUserName(), getUserEmail()
//
// OOP PRINCIPLE: INHERITANCE
// - Child class (DashboardActivity) extends Parent class (BaseActivity)
// - Inherits: getUserId(), getUserName(), getUserEmail()
// - Benefits: Code reuse, consistent data retrieval across all activities
// ============================================

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // USING INHERITED METHODS from BaseActivity
        val userId = getUserId()
        val userName = getUserName()
        val userEmail = getUserEmail()

        setContent {
            FitnesstrackerTheme {
                DashboardScreen(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userId: Int,
    userName: String,
    userEmail: String
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    var activities by remember { mutableStateOf<List<ActivityData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCalories by remember { mutableStateOf(0) }
    var totalDuration by remember { mutableStateOf(0) }
    var totalSessions by remember { mutableStateOf(0) }

    // Wellness data with safe defaults
    var screenTime by remember { mutableStateOf(0L) }
    var pickups by remember { mutableStateOf(0) }
    var averageUse by remember { mutableStateOf(0) }
    var continuousUse by remember { mutableStateOf(0) }
    var waterIntake by remember { mutableStateOf(0) }
    var waterGoal by remember { mutableStateOf(2500) }
    var hasScreenTimePermission by remember { mutableStateOf(false) }
    var isOverLimit by remember { mutableStateOf(false) }

    // Notification Permission for Android 13+
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission denied. You won't receive reminders.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Fetch activities on load
    LaunchedEffect(userId) {
        ApiClient.apiService.getActivities(userId).enqueue(object : Callback<ActivitiesResponse> {
            override fun onResponse(call: Call<ActivitiesResponse>, response: Response<ActivitiesResponse>) {
                isLoading = false
                val body = response.body()
                if (body != null && body.success) {
                    activities = body.activities
                    totalSessions = activities.size
                    totalCalories = activities.sumOf { it.calories }
                    totalDuration = activities.sumOf { it.duration }
                } else {
                    val errorMsg = body?.let { "Failed: ${it.activities}" } ?: "Failed to load activities: Unknown error"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActivitiesResponse>, t: Throwable) {
                isLoading = false
                val errorMsg = when (t) {
                    is java.net.ConnectException -> "Cannot connect to server. Check your network and IP address."
                    is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                    else -> "Error: ${t.localizedMessage}"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                Log.e("Dashboard", "Error: ${t.message}")
            }
        })
    }

    // Fetch wellness data with error handling
    LaunchedEffect(Unit) {
        try {
            val screenTimeHelper = ScreenTimeHelper(context)
            val waterTrackerHelper = WaterTrackerHelper(context)

            hasScreenTimePermission = screenTimeHelper.hasUsageStatsPermission()

            if (hasScreenTimePermission) {
                screenTime = screenTimeHelper.getTodayScreenTime()
                pickups = screenTimeHelper.getTodayPickups()
                averageUse = screenTimeHelper.getAverageSessionDuration()
                continuousUse = screenTimeHelper.getLongestSession()
                isOverLimit = screenTimeHelper.isOverScreenTimeLimit()
            }

            waterIntake = waterTrackerHelper.getTodayIntake()
            waterGoal = waterTrackerHelper.getDailyGoal()

        } catch (e: Exception) {
            Log.e("Dashboard", "Wellness error: ${e.message}")
        }
    }

    // Time-of-day greeting
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    context.startActivity(intent)
                },
                containerColor = primaryColor,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor,
                contentColor = onSurfaceColor
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
                        unselectedTextColor = Color.Gray
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
                        unselectedTextColor = Color.Gray
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
                        unselectedTextColor = Color.Gray
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
                        unselectedTextColor = Color.Gray
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
                // Header
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
                    val unreadCount = remember { mutableStateOf(notificationHelper.getUnreadCount()) }

                    LaunchedEffect(Unit) {
                        unreadCount.value = notificationHelper.getUnreadCount()
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

                        if (unreadCount.value > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(Color.Red, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount.value > 9) "9+" else "${unreadCount.value}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // --- NEW DESIGN: Integrated Dashboard ---

                // 1. Progress Overview Card (Circular Goal)
                SummaryOverviewCard(
                    totalCalories = totalCalories,
                    totalDuration = totalDuration,
                    totalSessions = totalSessions,
                    calorieGoal = 2500 // Default goal
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Wellness Quick Stats (Horizontal)
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
                            // Quick add water logic
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
                        value = "${screenTime / (1000 * 60 * 60)}h ${(screenTime / (1000 * 60)) % 60}m",
                        progress = if (screenTime > 0) (screenTime.toFloat() / (7 * 3600 * 1000)).coerceIn(0f, 1f) else 0f,
                        icon = Icons.Default.Smartphone,
                        color = if (isOverLimit) Color(0xFFFF5722) else primaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Navigate to detailed wellness if needed
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Recent Activities
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
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
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

@Composable
fun SummaryOverviewCard(
    totalCalories: Int,
    totalDuration: Int,
    totalSessions: Int,
    calorieGoal: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Progress on the left
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = (totalCalories.toFloat() / calorieGoal).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCalories",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "kcal",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Stats on the right
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(start = 24.dp)
            ) {
                SummaryStatRow(
                    label = "Workouts",
                    value = "$totalSessions",
                    icon = Icons.Default.FitnessCenter,
                    color = primaryColor
                )
                SummaryStatRow(
                    label = "Duration",
                    value = "${totalDuration}m",
                    icon = Icons.Default.Timer,
                    color = Color(0xFF4CAF50)
                )
                SummaryStatRow(
                    label = "Goal",
                    value = "$calorieGoal",
                    icon = Icons.Default.Flag,
                    color = Color(0xFFFF9800)
                )
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
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
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
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
fun StatItemSmall(
    label: String,
    value: String,
    icon: ImageVector,
    textColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = textColor.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun ActivityItem(activity: ActivityData) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (activity.activity_type) {
                "Running" -> Icons.Default.DirectionsRun
                "Cycling" -> Icons.Default.DirectionsBike
                "Swimming" -> Icons.Default.Pool
                "Walking" -> Icons.Default.DirectionsWalk
                else -> Icons.Default.FitnessCenter
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor.copy(alpha = 0.1f)),
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
                    text = "${activity.duration} min • ${String.format("%.1f", activity.distance)} km",
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