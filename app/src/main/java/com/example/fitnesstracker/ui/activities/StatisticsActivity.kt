package com.example.fitnesstracker.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme
import com.example.fitnesstracker.ui.viewmodel.StatisticsViewModel
import com.example.fitnesstracker.ui.viewmodel.ViewModelFactory

class StatisticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("FitnessTrackerPrefs", android.content.Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("USER_ID", 0)

        val repository = DataRepository(ApiClient.apiService)
        val factory = ViewModelFactory(application, repository)

        setContent {
            FitnesstrackerTheme {
                val viewModel: StatisticsViewModel = viewModel(factory = factory)
                StatisticsScreenContent(
                    userId = userId,
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun StatisticsScreenContent(
    userId: Int,
    viewModel: StatisticsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedFilter = viewModel.selectedFilter

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    // Re-fetch on every resume so new activities are reflected immediately.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && userId > 0) {
                viewModel.fetchActivities(userId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val filteredActivities = remember(activities, selectedFilter) {
        if (selectedFilter == TimeFilter.ALL_TIME) activities
        else {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            when (selectedFilter) {
                TimeFilter.WEEKLY -> cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                TimeFilter.MONTHLY -> cal.add(java.util.Calendar.MONTH, -1)
                else -> {}
            }
            val threshold = cal.time
            activities.filter {
                try {
                    val date = sdf.parse(it.created_at)
                    date != null && date.after(threshold)
                } catch (e: Exception) { true }
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(text = "Statistics", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(50.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimeFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter
                Box(
                    modifier = Modifier.weight(1f).background(if (selected) primaryColor else Color.Transparent, RoundedCornerShape(50.dp))
                        .clickable { viewModel.selectFilter(filter) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = filter.displayName, color = if (selected) Color.White else Color.Gray, fontSize = 13.sp)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatSummaryCard(Modifier.weight(1f), "Calories", totalCalories.toString(), "kcal", Icons.Default.LocalFireDepartment, Color(0xFFFF5722))
                StatSummaryCard(Modifier.weight(1f), "Time", totalMinutes.toString(), "min", Icons.Default.Timer, Color(0xFF2196F3))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatSummaryCard(Modifier.weight(1f), "Sessions", totalSessions.toString(), "total", Icons.Default.FitnessCenter, Color(0xFF4CAF50))
                StatSummaryCard(Modifier.weight(1f), "Distance", String.format(java.util.Locale.US, "%.1f", totalDistance), "km", Icons.Default.Straighten, Color(0xFF9C27B0))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Activity Overview", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            val chartData = viewModel.resolveChartData(filteredActivities)
            BarChart(chartData, primaryColor)

            Spacer(modifier = Modifier.height(24.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (filteredActivities.isEmpty()) {
                Text(text = "No activities recorded yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
            } else {
                filteredActivities.forEach { activity ->
                    ActivityItemCard(activity)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun BarChart(data: StatisticsViewModel.ChartDataSet, color: Color) {
    // Guard: when all buckets are 0, floor maxValue at 1 to prevent NaN from (0/0)
    val rawMax   = data.values.maxOrNull() ?: 0
    val maxValue = maxOf(rawMax, 1).toFloat()
    val allZero  = rawMax == 0

    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(data) { triggered = true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (allZero) {
                // Real zero-data state — chart is correct, not broken
                Box(
                    modifier         = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint               = Color.Gray.copy(alpha = 0.4f),
                            modifier           = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No activity in this period", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    val chartH = size.height
                    val barW   = size.width / (data.labels.size * 2)
                    data.values.forEachIndexed { i, value ->
                        val barH = (value / maxValue) * chartH
                        drawRoundRect(
                            color        = color,
                            topLeft      = Offset(i * barW * 2 + barW / 2, chartH - barH),
                            size         = Size(barW, barH),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                data.labels.forEach { Text(text = it, fontSize = 10.sp, color = Color.Gray) }
            }
        }
    }
}


@Composable
fun StatSummaryCard(modifier: Modifier, title: String, value: String, unit: String, icon: ImageVector, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "$unit $title", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActivityItemCard(activity: ActivityData) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.activity_type, fontWeight = FontWeight.Bold)
                Text(text = "${activity.duration} min • ${activity.distance} km", fontSize = 12.sp, color = Color.Gray)
            }
            Text(text = "${activity.calories} kcal", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

enum class TimeFilter(val displayName: String) { WEEKLY("Weekly"), MONTHLY("Monthly"), ALL_TIME("All Time") }