package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivitiesResponse
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.utils.BaseActivity

import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ============================================
// INHERITANCE EXAMPLE 1: StatisticsActivity extends BaseActivity
// ============================================
// This class DEMONSTRATES INHERITANCE by:
// 1. Extending BaseActivity (inherits helper methods)
// 2. Using inherited method getUserId() from BaseActivity
//
// OOP PRINCIPLE: INHERITANCE
// - Child class (StatisticsActivity) extends Parent class (BaseActivity)
// - Inherits: getUserId(), getUserName(), darkPurple, purple, etc.
// - Benefits: Code reuse, consistency, maintainability
// ============================================

class StatisticsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // USING INHERITED METHOD from BaseActivity
        // This demonstrates INHERITANCE - we don't need to redefine this method
        val userId = getUserId()

        setContent {
            FitnesstrackerTheme {
                StatisticsScreenContent(
                    userId = userId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreenContent(userId: Int, onBack: () -> Unit) {
    var activities by remember { mutableStateOf<List<ActivityData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(TimeFilter.ALL_TIME) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(userId) {
        ApiClient.apiService.getActivities(userId).enqueue(object : Callback<ActivitiesResponse> {
            override fun onResponse(call: Call<ActivitiesResponse>, response: Response<ActivitiesResponse>) {
                isLoading = false
                val body = response.body()
                if (body != null && body.success) {
                    activities = body.activities
                }
            }

            override fun onFailure(call: Call<ActivitiesResponse>, t: Throwable) {
                isLoading = false
                Log.e("Statistics", "Error: ${t.localizedMessage}")
            }
        })
    }

    // Dynamic Filter Logic
    val filteredActivities = remember(activities, selectedFilter) {
        if (selectedFilter == TimeFilter.ALL_TIME) {
            activities
        } else {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()
            when (selectedFilter) {
                TimeFilter.WEEKLY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                TimeFilter.MONTHLY -> calendar.add(java.util.Calendar.MONTH, -1)
                else -> {}
            }
            val thresholdDate = calendar.time
            activities.filter {
                try {
                    val date = dateFormat.parse(it.created_at)
                    date != null && date.after(thresholdDate)
                } catch (e: Exception) {
                    true // Include if parsing fails to avoid missing data
                }
            }
        }
    }

    val totalCalories = filteredActivities.sumOf { it.calories }
    val totalMinutes = filteredActivities.sumOf { it.duration }
    val totalSessions = filteredActivities.size
    val totalDistance = filteredActivities.sumOf { it.distance.toDouble() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Modern Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Dashboard",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Time Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { 
                        Text(
                            text = filter.displayName,
                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Stats Grid 
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Calories",
                        value = totalCalories.toString(),
                        unit = "kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        color = Color(0xFFFF5722)
                    )
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Time",
                        value = if (totalMinutes >= 60) String.format("%.1f", totalMinutes / 60f) else totalMinutes.toString(),
                        unit = if (totalMinutes >= 60) "hrs" else "min",
                        icon = Icons.Default.Timer,
                        color = Color(0xFF2196F3)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Sessions",
                        value = totalSessions.toString(),
                        unit = "total",
                        icon = Icons.Default.FitnessCenter,
                        color = Color(0xFF4CAF50)
                    )
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Distance",
                        value = String.format("%.1f", totalDistance),
                        unit = "km",
                        icon = Icons.Default.Straighten,
                        color = Color(0xFF9C27B0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (filteredActivities.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No activities found",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Adjust the filter or start tracking!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "Activity Breakdown",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                filteredActivities.forEach { activity ->
                    ActivityItemCard(activity = activity)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class TimeFilter(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ALL_TIME("All Time")
}

@Composable
fun StatSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(text = title, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ActivityItemCard(activity: ActivityData) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = activity.activity_type,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.activity_type,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${activity.duration} min • ${String.format("%.1f", activity.distance)} km",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${activity.calories}",
                    color = primaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
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