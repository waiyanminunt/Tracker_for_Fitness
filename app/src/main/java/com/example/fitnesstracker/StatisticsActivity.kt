package com.example.fitnesstracker

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
            StatisticsScreenContent(
                userId = userId,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun StatisticsScreenContent(userId: Int, onBack: () -> Unit) {
    var activities by remember { mutableStateOf<List<ActivityData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val darkPurple = Color(0xFF1A0A2E)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)

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
                Log.e("Statistics", "Error: ${t.message}")
            }
        })
    }

    val totalCalories = activities.sumOf { it.calories }
    val totalMinutes = activities.sumOf { it.duration }
    val totalSessions = activities.size
    val totalDistance = activities.sumOf { it.distance.toDouble() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkPurple)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column {
                Text(text = "Your Statistics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "Based on $totalSessions activities", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = lightPurple)
                }
            } else if (activities.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "No activities yet", color = Color.White, fontSize = 18.sp)
                        Text(text = "Start tracking to see statistics!", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                Text(text = "Activity Breakdown", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                activities.forEach { activity ->
                    ActivityItemCard(activity = activity)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
    val cardBg = Color(0xFF2D1B4E)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, color = Color.Gray, fontSize = 14.sp)
            }
            Text(text = title, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ActivityItemCard(activity: ActivityData) {
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
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = activity.activity_type, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.activity_type, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "${activity.duration} min • ${String.format("%.1f", activity.distance)} km", color = Color.Gray, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${activity.calories}", color = lightPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "kcal", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}