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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
            AddActivityScreen(
                userId = userId,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun AddActivityScreen(userId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedActivity by remember { mutableStateOf<String?>(null) }

    // Activities that need GPS tracking
    val gpsActivities = listOf("Running", "Cycling", "Walking")

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
                    Icons.Default.ArrowBack,
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
            // Activity Selection Screen
            ActivitySelectionGrid { activity ->
                selectedActivity = activity
            }
        } else {
            // Input Form Screen (for non-GPS activities: Swimming, Weightlifting)
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
    val surfaceColor = MaterialTheme.colorScheme.surface

    val activities = listOf(
        Triple("Running", Icons.Default.DirectionsRun, MaterialTheme.colorScheme.primary),
        Triple("Cycling", Icons.Default.DirectionsBike, Color(0xFF2196F3)),
        Triple("Swimming", Icons.Default.Pool, Color(0xFF00BCD4)),
        Triple("Weightlifting", Icons.Default.FitnessCenter, Color(0xFFFF9800)),
        Triple("Walking", Icons.Default.DirectionsWalk, Color(0xFF9C27B0))
    )

    // GPS activities
    val gpsActivities = listOf("Running", "Cycling", "Walking")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "What did you do today?",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Running, Cycling, Walking use GPS tracking",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        activities.forEach { (name, icon, color) ->
            ActivityCardLarge(
                name = name,
                icon = icon,
                color = color,
                hasGps = name in gpsActivities,
                onClick = { onActivitySelected(name) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ActivityCardLarge(
    name: String,
    icon: ImageVector,
    color: Color,
    hasGps: Boolean,
    onClick: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
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
                    contentDescription = name,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (hasGps) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "GPS",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS Tracking",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = Color.Gray
            )
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

    val calculatedCalories = remember(duration, distance, activityType) {
        // Only calculate if duration is provided
        if (duration.isNotEmpty()) {
            val durationInt = duration.toIntOrNull() ?: 0
            val distanceDouble = distance.toDoubleOrNull() ?: 0.0

            // POLYMORPHISM IN ACTION:
            // We create different workout objects, but call the SAME method
            when (activityType) {
                "Swimming" -> {
                    // Create CardioWorkout for Swimming
                    val workout = CardioWorkout(
                        id = 0,
                        name = "Swimming",
                        durationMinutes = durationInt,
                        distance = distanceDouble
                    )
                    workout.calculateCalories() // POLYMORPHISM: Uses MET 7.0
                }
                "Weightlifting" -> {
                    // Create StrengthWorkout for Weightlifting
                    val workout = StrengthWorkout(
                        id = 0,
                        name = "Weightlifting",
                        durationMinutes = durationInt,
                        sets = 3,    // Default sets
                        reps = 10    // Default reps
                    )
                    workout.calculateCalories() // POLYMORPHISM: Uses sets × reps formula
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

        Spacer(modifier = Modifier.height(16.dp))

        // Distance
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
                        distance = if (distance.isNotEmpty()) distance.toDouble() else 0.0,
                        calories = calculatedCalories, // Using POLYMORPHISM-calculated value!
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
                    color = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
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