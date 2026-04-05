package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivitiesResponse
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.utils.BaseActivity

import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import com.example.fitnesstracker.ui.viewmodel.StatisticsViewModel

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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

    // ViewModel is created once and survives screen rotation / config changes.
    // `by viewModels()` uses the Activity as the ViewModelStore owner.
    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // USING INHERITED METHOD from BaseActivity
        val userId = getUserId()

        setContent {
            FitnesstrackerTheme {
                // Pass the ViewModel instance down — the Composable reads state
                // from it and calls viewModel.selectFilter() on user interaction.
                StatisticsScreenContent(
                    userId    = userId,
                    viewModel = viewModel,
                    onBack    = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreenContent(
    userId: Int,
    viewModel: StatisticsViewModel,   // ← injected from Activity, survives rotation
    onBack: () -> Unit
) {
    var activities by remember { mutableStateOf<List<ActivityData>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Read filter from ViewModel — persists across rotation, back-stack, etc.
    val selectedFilter = viewModel.selectedFilter

    val primaryColor    = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor    = MaterialTheme.colorScheme.surface

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
                val errorMsg = when (t) {
                    is java.net.ConnectException       -> "Cannot connect to server. Check your network."
                    is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                    else                               -> "Error: " + (t.localizedMessage ?: "Unknown error")
                }
                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                Log.e("Statistics", "Error: " + t.localizedMessage)
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

        // ── Global segmented filter — single source of truth ───────────────
        val isDarkTop = MaterialTheme.colorScheme.background.red < 0.5f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = if (isDarkTop) Color.White.copy(alpha = 0.07f)
                            else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimeFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) primaryColor else Color.Transparent,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .clickable { viewModel.selectFilter(filter) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = filter.displayName,
                        color      = if (selected) Color.White
                                     else if (isDarkTop) Color.White.copy(alpha = 0.55f)
                                     else Color.Black.copy(alpha = 0.45f),
                        fontSize   = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
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

            // ── Calories Bar Chart (with time filter) ───────────────────
            Text(
                text = "Calories Overview",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            CaloriesBarChart(
                activities   = filteredActivities,
                selectedFilter = selectedFilter,
                goalCalories = 500
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Insight Summary Row ─────────────────────────────────────
            val weeklyAvg = if (filteredActivities.isEmpty()) 0
                else filteredActivities.sumOf { it.calories } / filteredActivities.size
            val bestDay = filteredActivities.maxOfOrNull { it.calories } ?: 0
            val goalHits = filteredActivities.count { it.calories >= 500 }
            val goalRate = if (filteredActivities.isEmpty()) 0
                else ((goalHits.toFloat() / filteredActivities.size) * 100).toInt()

            InsightSummaryRow(
                weeklyAvg = weeklyAvg,
                bestDay = bestDay,
                goalRate = goalRate
            )

            Spacer(modifier = Modifier.height(28.dp))

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
        "Running"      -> Icons.AutoMirrored.Filled.DirectionsRun  to Color(0xFF4CAF50)
        "Cycling"      -> Icons.AutoMirrored.Filled.DirectionsBike to Color(0xFF2196F3)
        "Swimming"     -> Icons.Default.Pool                        to Color(0xFF00BCD4)
        "Weightlifting"-> Icons.Default.FitnessCenter               to Color(0xFFFF9800)
        "Walking"      -> Icons.AutoMirrored.Filled.DirectionsWalk  to Color(0xFF9C27B0)
        else           -> Icons.Default.FitnessCenter               to Color(0xFF607D8B)
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

// ─────────────────────────────────────────────────────────────────────────────
// CALORIES BAR CHART — pure Canvas, built-in time filter, smooth animations
// ─────────────────────────────────────────────────────────────────────────────

// (ChartFilter removed — CaloriesBarChart now receives TimeFilter from the parent)

@Composable
fun CaloriesBarChart(
    activities: List<ActivityData>,
    selectedFilter: TimeFilter,        // ← hoisted from parent, single source of truth
    goalCalories: Int = 500
) {
    // ── Theme tokens ────────────────────────────────────────────────────────
    val primaryColor   = MaterialTheme.colorScheme.primary
    val surfaceColor   = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isDark         = MaterialTheme.colorScheme.background.red < 0.5f
    val gridLineColor  = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val labelColor     = if (isDark) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.45f)
    val goalLineColor  = primaryColor.copy(alpha = 0.85f)

    // ── Bar palette (cycles if bar count > 7) ──────────────────────────────
    val palette = listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEAB308),
        Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF8B5CF6),
        Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF43F5E),
        Color(0xFF84CC16), Color(0xFF06B6D4), Color(0xFFA855F7)
    )

    // ── 3 distinct mock data sets ───────────────────────────────────────────
    val mockWeekly  = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") to
                      intArrayOf(320, 480, 210, 550, 390, 620, 445)
    val mockMonthly = listOf("W1", "W2", "W3", "W4") to
                      intArrayOf(1840, 2650, 980, 3120)
    val mockAllTime = listOf("Jan","Feb","Mar","Apr","May","Jun",
                             "Jul","Aug","Sep","Oct","Nov","Dec") to
                      intArrayOf(4200,3800,5100,4700,6200,5800,7100,6600,5300,4900,6800,7500)

    // ── Compute real-data buckets, fall back to mock if empty ───────────────
    fun realWeekly(): IntArray {
        val buckets = IntArray(7)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        activities.forEach { act ->
            try {
                val d   = sdf.parse(act.created_at) ?: return@forEach
                val cal = java.util.Calendar.getInstance().apply { time = d }
                val idx = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                buckets[idx] += act.calories
            } catch (_: Exception) {}
        }
        return if (buckets.all { it == 0 }) mockWeekly.second else buckets
    }

    fun realMonthly(): IntArray {
        val buckets = IntArray(4)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val now = java.util.Calendar.getInstance()
        activities.forEach { act ->
            try {
                val d   = sdf.parse(act.created_at) ?: return@forEach
                val cal = java.util.Calendar.getInstance().apply { time = d }
                val daysAgo = ((now.timeInMillis - cal.timeInMillis) / 86_400_000).toInt()
                val week    = (daysAgo / 7).coerceIn(0, 3)
                buckets[3 - week] += act.calories
            } catch (_: Exception) {}
        }
        return if (buckets.all { it == 0 }) mockMonthly.second else buckets
    }

    fun realAllTime(): IntArray {
        val buckets = IntArray(12)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        activities.forEach { act ->
            try {
                val d   = sdf.parse(act.created_at) ?: return@forEach
                val cal = java.util.Calendar.getInstance().apply { time = d }
                buckets[cal.get(java.util.Calendar.MONTH)] += act.calories
            } catch (_: Exception) {}
        }
        return if (buckets.all { it == 0 }) mockAllTime.second else buckets
    }

    // ── Map parent TimeFilter → chart labels + values (single source of truth) ──
    val (labels, rawValues) = when (selectedFilter) {
        TimeFilter.WEEKLY   -> mockWeekly.first  to realWeekly()
        TimeFilter.MONTHLY  -> mockMonthly.first to realMonthly()
        TimeFilter.ALL_TIME -> mockAllTime.first to realAllTime()
    }
    val maxValue = maxOf(rawValues.max(), goalCalories).toFloat()

    // ── Animate bars whenever selectedFilter changes ──────────────────────────
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(selectedFilter) {
        triggered = false
        kotlinx.coroutines.delay(50)
        triggered = true
    }
    val animatedScales = rawValues.map { raw ->
        animateFloatAsState(
            targetValue   = if (triggered) (raw / maxValue).coerceIn(0f, 1f) else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            ),
            label = "barScale"
        )
    }

    // ── UI ──────────────────────────────────────────────────────────────────
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.7f)),
        border    = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Legend row: title + goal marker ──────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Calories Burned",
                    color      = onSurfaceColor,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(goalLineColor, goalLineColor.copy(alpha = 0f))
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = "Goal $goalCalories kcal",
                        color    = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Canvas chart (no pill selector here — filter is controlled by parent) ──
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val chartH    = size.height
                val chartW    = size.width
                val barCount  = labels.size
                val totalSlots = barCount * 2f
                val barWidth  = (chartW / totalSlots).coerceAtLeast(8f)
                val spacing   = barWidth
                val bottomPad = 28f
                val topPad    = 12f
                val drawH     = chartH - bottomPad - topPad

                // Grid lines
                for (i in 1..4) {
                    val y = topPad + drawH * (1f - i / 4f)
                    drawLine(
                        color       = gridLineColor,
                        start       = Offset(0f, y),
                        end         = Offset(chartW, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Goal dashed line
                val goalY = topPad + drawH * (1f - goalCalories / maxValue)
                drawLine(
                    color       = goalLineColor,
                    start       = Offset(0f, goalY),
                    end         = Offset(chartW, goalY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Animated gradient bars
                labels.forEachIndexed { i, _ ->
                    val barColor = palette[i % palette.size]
                    val left     = spacing / 2f + i * (barWidth + spacing)
                    val scale    = animatedScales[i].value
                    val barH     = drawH * scale
                    val top      = topPad + drawH - barH

                    if (barH > 0f) {
                        drawRoundRect(
                            brush        = Brush.verticalGradient(
                                colors   = listOf(barColor, barColor.copy(alpha = 0.40f)),
                                startY   = top,
                                endY     = topPad + drawH
                            ),
                            topLeft      = Offset(left, top),
                            size         = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            // ── X-axis labels ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                labels.forEach { lbl ->
                    Text(
                        text       = lbl,
                        color      = labelColor,
                        fontSize   = if (labels.size > 7) 9.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INSIGHT SUMMARY ROW — three mini stat chips below the chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InsightSummaryRow(weeklyAvg: Int, bestDay: Int, goalRate: Int) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InsightChip(
            modifier = Modifier.weight(1f),
            label    = "Avg / Session",
            value    = "${weeklyAvg} kcal",
            icon     = Icons.AutoMirrored.Filled.ShowChart,
            color    = Color(0xFF3B82F6),
            surface  = surfaceColor
        )
        InsightChip(
            modifier = Modifier.weight(1f),
            label    = "Best Session",
            value    = "${bestDay} kcal",
            icon     = Icons.Default.EmojiEvents,
            color    = Color(0xFFF59E0B),
            surface  = surfaceColor
        )
        InsightChip(
            modifier = Modifier.weight(1f),
            label    = "Goal Hit Rate",
            value    = "${goalRate}%",
            icon     = Icons.Default.TrackChanges,
            color    = Color(0xFF22C55E),
            surface  = surfaceColor
        )
    }
}

@Composable
fun InsightChip(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    surface: Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = surface.copy(alpha = 0.6f)),
        border    = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier              = Modifier
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector     = icon,
                    contentDescription = label,
                    tint            = color,
                    modifier        = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = value,
                color      = MaterialTheme.colorScheme.onSurface,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = label,
                color    = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}