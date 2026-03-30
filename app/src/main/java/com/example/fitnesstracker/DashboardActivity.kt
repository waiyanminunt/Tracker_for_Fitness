package com.example.fitnesstracker

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
            DashboardScreen(
                userId = userId,
                userName = userName,
                userEmail = userEmail
            )
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
    var selectedDashboardTab by remember { mutableStateOf(0) }

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

    val darkPurple = Color(0xFF1A0A2E)
    val purple = Color(0xFF6B4C9A)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)

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
                    Toast.makeText(context, "Failed to load activities", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActivitiesResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("Dashboard", "Error: ${t.message}")
            }
        })
    }

    // Fetch wellness data with error handling
    LaunchedEffect(selectedDashboardTab) {
        if (selectedDashboardTab == 1) {
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
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    context.startActivity(intent)
                },
                containerColor = purple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = darkPurple,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = lightPurple,
                        selectedTextColor = lightPurple,
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
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    label = { Text("Add") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = lightPurple,
                        selectedTextColor = lightPurple,
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
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = lightPurple,
                        selectedTextColor = lightPurple,
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
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = lightPurple,
                        selectedTextColor = lightPurple,
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
                .background(darkPurple)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                            text = "Hello, $userName!",
                            color = Color.White,
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

// Update unread count when tab changes
                    LaunchedEffect(selectedDashboardTab) {
                        unreadCount.value = notificationHelper.getUnreadCount()
                    }

                    Box {
                        IconButton(onClick = {
                            Toast.makeText(context, "You have ${unreadCount.value} unread notification${if (unreadCount.value != 1) "s" else ""}", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, NotificationsActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }

                        // Red badge for unread count
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

                // Dashboard Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TabButton(
                        text = "Activities",
                        selected = selectedDashboardTab == 0,
                        onClick = { selectedDashboardTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TabButton(
                        text = "Wellness",
                        selected = selectedDashboardTab == 1,
                        onClick = { selectedDashboardTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (selectedDashboardTab == 0) {
                        ActivitiesContent(
                            isLoading = isLoading,
                            activities = activities,
                            totalSessions = totalSessions,
                            totalCalories = totalCalories,
                            totalDuration = totalDuration
                        )
                    } else {
                        WellnessContent(
                            hasPermission = hasScreenTimePermission,
                            screenTime = screenTime,
                            pickups = pickups,
                            averageUse = averageUse,
                            continuousUse = continuousUse,
                            isOverLimit = isOverLimit,
                            waterIntake = waterIntake,
                            waterGoal = waterGoal,
                            onGrantPermission = {
                                try {
                                    val helper = ScreenTimeHelper(context)
                                    helper.openUsageStatsSettings()
                                } catch (e: Exception) {
                                    Log.e("Dashboard", "Error opening settings: ${e.message}")
                                }
                            },
                            onWaterAdded = { amount ->
                                try {
                                    val waterHelper = WaterTrackerHelper(context)
                                    waterHelper.addWater(amount)
                                    waterIntake = waterHelper.getTodayIntake()
                                } catch (e: Exception) {
                                    Log.e("Dashboard", "Error adding water: ${e.message}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purple = Color(0xFF6B4C9A)
    val cardBg = Color(0xFF2D1B4E)

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) purple else cardBg
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else Color.Gray,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ActivitiesContent(
    isLoading: Boolean,
    activities: List<ActivityData>,
    totalSessions: Int,
    totalCalories: Int,
    totalDuration: Int
) {
    val purple = Color(0xFF6B4C9A)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = purple)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Your Progress",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItemSmall("Workouts", "$totalSessions", Icons.Default.FitnessCenter)
                    StatItemSmall("Calories", "$totalCalories", Icons.Default.LocalFireDepartment)
                    StatItemSmall("Minutes", "$totalDuration", Icons.Default.Timer)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Activities",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = lightPurple)
            }
        } else if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No activities yet",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Tap + to add your first workout!",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activities) { activity ->
                    ActivityCardReal(activity)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun WellnessContent(
    hasPermission: Boolean,
    screenTime: Long,
    pickups: Int,
    averageUse: Int,
    continuousUse: Int,
    isOverLimit: Boolean,
    waterIntake: Int,
    waterGoal: Int,
    onGrantPermission: () -> Unit,
    onWaterAdded: (Int) -> Unit
) {
    val purple = Color(0xFF6B4C9A)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)
    val blueColor = Color(0xFF2196F3)
    val orangeColor = Color(0xFFFF9800)
    val redColor = Color(0xFFE53935)
    val greenColor = Color(0xFF4CAF50)

    val waterProgress = if (waterGoal > 0) {
        ((waterIntake.toFloat() / waterGoal) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Screen Time Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱 Screen Time",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!hasPermission) {
                        TextButton(onClick = onGrantPermission) {
                            Text(
                                text = "Grant Permission",
                                color = lightPurple,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (hasPermission) {
                    // Format screen time
                    val hours = screenTime / (1000 * 60 * 60)
                    val minutes = (screenTime / (1000 * 60)) % 60
                    val screenTimeText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WellnessStatItem(
                            label = "Screen Time",
                            value = screenTimeText,
                            icon = Icons.Default.Timer,
                            color = if (isOverLimit) redColor else blueColor
                        )
                        WellnessStatItem(
                            label = "Pickups",
                            value = "$pickups",
                            icon = Icons.Default.TouchApp,
                            color = orangeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WellnessStatItem(
                            label = "Avg Use",
                            value = "${averageUse}m",
                            icon = Icons.Default.QueryStats,
                            color = purple
                        )
                        WellnessStatItem(
                            label = "Continuous",
                            value = "${continuousUse}m",
                            icon = Icons.Default.HourglassTop,
                            color = if (continuousUse > 60) redColor else greenColor
                        )
                    }

                    if (isOverLimit) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = redColor.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = redColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Over 7 hours! Take a break!",
                                    color = redColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Grant permission to track screen time",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Drink Water Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "💧 Drink Water",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${waterIntake}ml · $waterProgress%",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Remaining: ${((waterGoal - waterIntake).coerceAtLeast(0))}ml",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    if (waterProgress >= 100) {
                        Text(
                            text = "Goal reached!",
                            color = greenColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { waterProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = blueColor,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quick Add",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WaterButton("50ml", 50, onWaterAdded, blueColor)
                    WaterButton("150ml", 150, onWaterAdded, blueColor)
                    WaterButton("250ml", 250, onWaterAdded, blueColor)
                    WaterButton("500ml", 500, onWaterAdded, blueColor)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun WellnessStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
fun WaterButton(
    text: String,
    amount: Int,
    onWaterAdded: (Int) -> Unit,
    color: Color
) {
    Button(
        onClick = { onWaterAdded(amount) },
        modifier = Modifier.size(70.dp, 36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatItemSmall(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun ActivityCardReal(activity: ActivityData) {
    val cardBg = Color(0xFF2D1B4E)
    val lightPurple = Color(0xFF9B7DD4)

    val (icon, color) = when (activity.activity_type) {
        "Running" -> Icons.Default.DirectionsRun to Color(0xFF4CAF50)
        "Cycling" -> Icons.Default.DirectionsBike to Color(0xFF2196F3)
        "Swimming" -> Icons.Default.Pool to Color(0xFF00BCD4)
        "Weightlifting" -> Icons.Default.FitnessCenter to Color(0xFFFF9800)
        "Walking" -> Icons.Default.DirectionsWalk to Color(0xFF9C27B0)
        else -> Icons.Default.FitnessCenter to Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = activity.activity_type,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.activity_type,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${activity.duration} min · ${String.format("%.1f", activity.distance)} km",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${activity.calories}",
                    color = lightPurple,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "kcal",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}