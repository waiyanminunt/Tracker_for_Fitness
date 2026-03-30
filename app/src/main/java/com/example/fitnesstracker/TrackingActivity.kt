package com.example.fitnesstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TrackingActivity : ComponentActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val activityType = intent.getStringExtra("ACTIVITY_TYPE") ?: "Running"
        val userId = intent.getIntExtra("USER_ID", 0)

        setContent {
            TrackingScreen(
                activityType = activityType,
                userId = userId,
                onBack = { finish() },
                onFinish = { distance, duration, calories ->
                    finish()
                }
            )
        }
    }
}

@Composable
fun TrackingScreen(
    activityType: String,
    userId: Int,
    onBack: () -> Unit,
    onFinish: (distance: Float, duration: Long, calories: Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as TrackingActivity

    var isTracking by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf(0L) }
    var distanceMeters by remember { mutableStateOf(0f) }
    var currentSpeed by remember { mutableStateOf(0f) }
    var hasPermission by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val darkPurple = Color(0xFF1A0A2E)
    val purple = Color(0xFF6B4C9A)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)

    // Permission launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Check and request permission
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasPermission = fineLocation || coarseLocation

        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Timer
    LaunchedEffect(isTracking) {
        while (isTracking) {
            kotlinx.coroutines.delay(1000)
            durationSeconds++
        }
    }

    // Location tracking
    LaunchedEffect(isTracking, hasPermission) {
        if (isTracking && hasPermission) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            ).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        currentSpeed = location.speed
                        if (isTracking) {
                            distanceMeters += location.speed
                        }
                    }
                }
            }

            try {
                activity.fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                // Handle permission issue
            }
        }
    }

    // Format time
    fun formatTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    // Format distance
    fun formatDistance(meters: Float): String {
        val km = meters / 1000f
        return String.format("%.2f", km)
    }

    // Calculate pace (min/km)
    fun calculatePace(): String {
        if (distanceMeters < 100) return "--:--"
        val km = distanceMeters / 1000f
        val minutesPerKm = (durationSeconds / 60f) / km
        val mins = minutesPerKm.toInt()
        val secs = ((minutesPerKm - mins) * 60).toInt()
        return String.format("%d:%02d", mins, secs)
    }

    // Calculate calories (simplified)
    fun calculateCalories(): Int {
        // Approximate: 60 cal per km for running, adjust for activity type
        val km = distanceMeters / 1000f
        val caloriesPerKm = when (activityType) {
            "Running" -> 60
            "Cycling" -> 30
            "Walking" -> 40
            else -> 50
        }
        return (km * caloriesPerKm).toInt()
    }

    // Save activity to database
    fun saveActivity() {
        isSaving = true
        val calories = calculateCalories()

        val request = ActivityRequest(
            user_id = userId,
            activity_type = activityType,
            duration = durationSeconds.toInt() / 60, // Convert to minutes
            distance = distanceMeters / 1000.0, // Convert to km
            calories = calories,
            notes = "Tracked via GPS"
        )

        ApiClient.apiService.saveActivity(request).enqueue(object : Callback<ActivityResponse> {
            override fun onResponse(call: Call<ActivityResponse>, response: Response<ActivityResponse>) {
                isSaving = false
                val body = response.body()
                if (body != null && body.success) {
                    Toast.makeText(context, "Activity saved!", Toast.LENGTH_SHORT).show()
                    onFinish(distanceMeters, durationSeconds, calories)
                } else {
                    Toast.makeText(context, body?.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActivityResponse>, t: Throwable) {
                isSaving = false
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Activity icon
    val activityIcon = when (activityType) {
        "Running" -> Icons.Default.DirectionsRun
        "Cycling" -> Icons.Default.DirectionsBike
        "Walking" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.DirectionsRun
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkPurple)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Icon(
                imageVector = activityIcon,
                contentDescription = activityType,
                tint = lightPurple,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activityType,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Permission denied message
        if (!hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Location permission required",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Please grant location permission to track your activity",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Main tracking content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Map placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = "Map",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "GPS Tracking Active",
                                color = if (isTracking) Color(0xFF4CAF50) else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer
                Text(
                    text = formatTime(durationSeconds),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isTracking) "In Progress" else "Ready",
                    color = if (isTracking) Color(0xFF4CAF50) else Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox(
                        label = "Distance",
                        value = formatDistance(distanceMeters),
                        unit = "km"
                    )
                    StatBox(
                        label = "Pace",
                        value = calculatePace(),
                        unit = "min/km"
                    )
                    StatBox(
                        label = "Speed",
                        value = String.format("%.1f", currentSpeed * 3.6f),
                        unit = "km/h"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Control Buttons
                if (!isTracking) {
                    // Start Button
                    Button(
                        onClick = { isTracking = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START $activityType".uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Stop Button
                    Button(
                        onClick = {
                            isTracking = false
                            saveActivity()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FINISH",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    unit: String
) {
    val cardBg = Color(0xFF2D1B4E)

    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}