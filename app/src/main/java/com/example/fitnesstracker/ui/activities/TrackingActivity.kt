package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivityRequest
import com.example.fitnesstracker.data.network.ActivityResponse
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme
import com.example.fitnesstracker.utils.StatBox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

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
    DisposableEffect(isTracking, hasPermission) {
        var locationCallback: LocationCallback? = null

        if (isTracking && hasPermission) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            ).build()

            locationCallback = object : LocationCallback() {
                private var lastLocation: android.location.Location? = null

                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        currentSpeed = location.speed
                        
                        if (lastLocation != null) {
                            val distance = lastLocation!!.distanceTo(location)
                            distanceMeters += distance
                        }
                        lastLocation = location
                    }
                }
            }

            try {
                activity.fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("Tracking", "Permission error: ${e.message}")
            }
        }

        onDispose {
            locationCallback?.let {
                activity.fusedLocationClient.removeLocationUpdates(it)
            }
        }
    }

    // Format time
    val formattedTime = remember(durationSeconds) {
        val hrs = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60
        if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    // Format distance
    val formattedDistance = remember(distanceMeters) {
        val km = distanceMeters / 1000f
        String.format("%.2f", km)
    }

    // Calculate pace (min/km)
    val pace = remember(durationSeconds, distanceMeters) {
        if (distanceMeters < 100) "--:--"
        else {
            val km = distanceMeters / 1000f
            val minutesPerKm = (durationSeconds / 60f) / km
            val mins = minutesPerKm.toInt()
            val secs = ((minutesPerKm - mins) * 60).toInt()
            String.format("%d:%02d", mins, secs)
        }
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
        else -> Icons.Default.Favorite // Changed to Favorite for a better match with the heart theme
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
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Icon(
                imageVector = activityIcon,
                contentDescription = activityType,
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activityType,
                color = MaterialTheme.colorScheme.onBackground,
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
                        color = MaterialTheme.colorScheme.onBackground,
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
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
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
                                color = if (isTracking) MaterialTheme.colorScheme.secondary else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer
                Text(
                    text = formattedTime,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isTracking) "In Progress" else "Ready",
                    color = if (isTracking) MaterialTheme.colorScheme.secondary else Color.Gray,
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
                        value = formattedDistance,
                        unit = "km",
                        cardBg = surfaceColor
                    )
                    StatBox(
                        label = "Pace",
                        value = pace,
                        unit = "min/km",
                        cardBg = surfaceColor
                    )
                    StatBox(
                        label = "Speed",
                        value = String.format("%.1f", currentSpeed * 3.6f),
                        unit = "km/h",
                        cardBg = surfaceColor
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START $activityType".uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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