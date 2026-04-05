package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivityRequest
import com.example.fitnesstracker.data.network.ActivityResponse
import com.example.fitnesstracker.utils.BaseActivity
import com.example.fitnesstracker.utils.CommonHeader
import com.example.fitnesstracker.data.models.CardioWorkout
import com.example.fitnesstracker.data.models.StrengthWorkout
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ============================================
// INHERITANCE EXAMPLE 4: AddActivity extends BaseActivity
// ============================================
// This class DEMONSTRATES INHERITANCE by:
// 1. Extending BaseActivity (inherits helper methods)
// 2. Using inherited method: getUserId()
//
// OOP PRINCIPLE: INHERITANCE
// - Child class (AddActivity) extends Parent class (BaseActivity)
// - Inherits: getUserId()
// ============================================

// ============================================
// POLYMORPHISM DEMONSTRATION in AddActivity
// ============================================
// When user selects a workout type, we create a workout object:
// - CardioWorkout for Running, Cycling, Swimming, Walking
// - StrengthWorkout for Weightlifting
//
// Both classes have the SAME method: calculateCalories()
// But each calculates calories DIFFERENTLY:
// - CardioWorkout: uses MET values (Running=9.8, Cycling=6.8, Walking=3.5)
// - StrengthWorkout: uses sets × reps formula
//
// This is POLYMORPHISM - same method, different behavior!
// ============================================

class AddActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // USING INHERITED METHOD from BaseActivity
        val userId = getUserId()

        setContent {
            FitnesstrackerTheme {
                AddActivityScreen(
                    userId = userId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AddActivityScreen(userId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedActivity by remember { mutableStateOf<String?>(null) }

    // Activities that need GPS tracking (Updated to include Hiking)
    val gpsActivities = listOf("Running", "Cycling", "Walking", "Hiking")

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    // Navigate to TrackingActivity if GPS activity is selected
    LaunchedEffect(selectedActivity) {
        if (selectedActivity != null && selectedActivity in gpsActivities) {
            val intent = Intent(context, TrackingActivity::class.java)
            intent.putExtra("ACTIVITY_TYPE", selectedActivity)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
            selectedActivity = null // Reset
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedActivity != null) {
                    selectedActivity = null
                } else {
                    onBack()
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (selectedActivity == null) "Select Activity" else "Add $selectedActivity",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (selectedActivity == null) {
            // Activity Selection Screen (Revamped to Grid)
            ActivitySelectionGrid { activity ->
                selectedActivity = activity
            }
        } else {
            // Input Form Screen (for non-GPS activities)
            ActivityInputForm(
                userId = userId,
                activityType = selectedActivity!!,
                onSave = {
                    selectedActivity = null
                }
            )
        }
    }
}

@Composable
fun ActivitySelectionGrid(onActivitySelected: (String) -> Unit) {
    // New Order & New Activities Added
    val activities = listOf(
        Triple("Cycling", Icons.AutoMirrored.Filled.DirectionsBike, Color(0xFF2196F3)),
        Triple("Hiking", Icons.Default.Terrain, Color(0xFF4CAF50)),
        Triple("Yoga", Icons.Default.SelfImprovement, Color(0xFFE91E63)),
        Triple("Swimming", Icons.Default.Pool, Color(0xFF00BCD4)),
        Triple("Running", Icons.AutoMirrored.Filled.DirectionsRun, MaterialTheme.colorScheme.primary),
        Triple("Walking", Icons.AutoMirrored.Filled.DirectionsWalk, Color(0xFF9C27B0)),
        Triple("Weightlifting", Icons.Default.FitnessCenter, Color(0xFFFF9800)),
        Triple("Football", Icons.Default.SportsSoccer, Color(0xFF2E7D32))
    )

    // GPS activities list for the UI tag
    val gpsActivities = listOf("Cycling", "Hiking", "Running", "Walking")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)
            ) {
                items(activities.size) { index ->
                    val (name, icon, color) = activities[index]
                    ActivityCardGrid(
                        name = name,
                        icon = icon,
                        color = color,
                        hasGps = name in gpsActivities,
                        onClick = { onActivitySelected(name) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityCardGrid(
    name: String,
    icon: ImageVector,
    color: Color,
    hasGps: Boolean,
    onClick: () -> Unit
) {
    // Create a slightly lighter color for the dark theme popping effect
    // Alternatively, you can use the Surface color with an elevated shadow
    val elevatedSurfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp), // Increased corner radius
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Slight shadow for depth
        colors = CardDefaults.cardColors(containerColor = elevatedSurfaceColor)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Larger Colorful Icon at the top
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = color,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activity Title
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // GPS Tracking Subtitle placed directly below if relevant
            if (hasGps) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "GPS",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GPS Tracking",
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // To maintain grid symmetry, we optionally add an empty space
                // or just let it adjust dynamically.
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ActivityInputForm(
    userId: Int,
    activityType: String,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary

    // ============================================
    // POLYMORPHISM DEMONSTRATION
    // ============================================
    // Calculate calories automatically using BaseWorkout classes
    // Same method name (calculateCalories), different implementation per workout type!

    // Activities that do NOT use distance — hide the field entirely for these
    val noDistanceActivities = remember { listOf("Yoga", "Weightlifting", "Football") }

    val calculatedCalories = remember(duration, distance, activityType) {
        if (duration.isNotEmpty()) {
            val durationInt    = duration.toIntOrNull() ?: 0
            val distanceDouble = distance.toDoubleOrNull() ?: 0.0

            // POLYMORPHISM IN ACTION:
            // We create different workout objects, but call the SAME method
            when (activityType) {
                "Swimming" -> {
                    val workout = CardioWorkout(
                        id = 0, name = "Swimming",
                        durationMinutes = durationInt, distance = distanceDouble
                    )
                    workout.calculateCalories() // MET 7.0
                }
                "Weightlifting" -> {
                    val workout = StrengthWorkout(
                        id = 0, name = "Weightlifting",
                        durationMinutes = durationInt, sets = 3, reps = 10
                    )
                    workout.calculateCalories() // sets × reps formula
                }
                "Yoga" -> {
                    // MET 2.8 × 3.5 × 70 kg / 200 × duration
                    ((2.8 * 3.5 * 70.0 / 200.0) * durationInt).toInt()
                }
                "Football" -> {
                    // MET 7.0 × 3.5 × 70 kg / 200 × duration
                    ((7.0 * 3.5 * 70.0 / 200.0) * durationInt).toInt()
                }
                else -> 0
            }
        } else {
            0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Show auto-calculated calories
        if (duration.isNotEmpty() && calculatedCalories > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Auto-Calculated Calories",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$calculatedCalories kcal",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Based on $activityType MET values",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Duration
        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it },
            label = { Text("Duration (minutes)", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.Timer, contentDescription = null, tint = primaryColor)
            }
        )

        // Distance — hidden entirely for non-distance activities (Yoga, Weightlifting)
        if (activityType !in noDistanceActivities) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = distance,
                onValueChange = { distance = it },
                label = { Text("Distance (km) - optional", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = primaryColor)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                if (duration.isNotEmpty()) {
                    isSaving = true

                    // Use calculated calories from BaseWorkout (POLYMORPHISM)
                    val request = ActivityRequest(
                        user_id = userId,
                        activity_type = activityType,
                        duration = duration.toInt(),
                        // Force 0 for non-distance activities so dashboard Total Distance stays clean
                        distance = if (activityType in noDistanceActivities) 0.0
                                   else if (distance.isNotEmpty()) distance.toDouble() else 0.0,
                        calories = calculatedCalories,
                        notes = notes
                    )

                    ApiClient.apiService.saveActivity(request).enqueue(object : Callback<ActivityResponse> {
                        override fun onResponse(call: Call<ActivityResponse>, response: Response<ActivityResponse>) {
                            isSaving = false
                            val body = response.body()
                            if (body != null && body.success) {
                                Toast.makeText(context, "Activity saved! ($calculatedCalories kcal)", Toast.LENGTH_SHORT).show()
                                onSave()
                            } else {
                                Toast.makeText(context, body?.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ActivityResponse>, t: Throwable) {
                            isSaving = false
                            val errorMsg = when (t) {
                                is java.net.ConnectException -> "Cannot connect to server. Check your network and IP address."
                                is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                                else -> "Error: ${t.localizedMessage}"
                            }
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    Toast.makeText(context, "Please enter duration", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE ACTIVITY",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}