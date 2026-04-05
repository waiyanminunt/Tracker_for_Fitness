package com.example.fitnesstracker.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivityRequest
import com.example.fitnesstracker.data.network.ActivityResponse
import com.example.fitnesstracker.utils.StatBox

// ─────────────────────────────────────────────────────────────────────────────
// TrackingState Enum
// ─────────────────────────────────────────────────────────────────────────────
enum class TrackingState { IDLE, RUNNING, PAUSED, STOPPED }

// ─────────────────────────────────────────────────────────────────────────────
// TrackingActivity - Entry Point
// ─────────────────────────────────────────────────────────────────────────────
class TrackingActivity : ComponentActivity() {

    internal lateinit var fusedLocationClient: FusedLocationProviderClient

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
                onFinish = { _, _, _ -> finish() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TrackingScreen - Main Composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrackingScreen(
    activityType: String,
    userId: Int,
    onBack: () -> Unit,
    onFinish: (distance: Float, duration: Long, calories: Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as TrackingActivity
    val isDark = isSystemInDarkTheme()

    // ── State ─────────────────────────────────────────────────────────────────
    var trackingState by remember { mutableStateOf(TrackingState.IDLE) }
    val isRunning = trackingState == TrackingState.RUNNING

    var durationSeconds by remember { mutableLongStateOf(0L) }
    var distanceMeters by remember { mutableFloatStateOf(0f) }
    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var hasPermission by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var locationPoints by remember { mutableStateOf(listOf<android.location.Location>()) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    // ── Permissions ───────────────────────────────────────────────────────────
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        hasPermission = fine || coarse
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────
    LaunchedEffect(isRunning) {
        while (isRunning) {
            kotlinx.coroutines.delay(1000)
            durationSeconds++
        }
    }

    // ── GPS ───────────────────────────────────────────────────────────────────
    DisposableEffect(isRunning, hasPermission) {
        var locationCallback: LocationCallback? = null

        if (isRunning && hasPermission) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            ).build()

            locationCallback = object : LocationCallback() {
                private var lastLocation: android.location.Location? = null
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        currentSpeed = loc.speed
                        lastLocation?.let { distanceMeters += it.distanceTo(loc) }
                        lastLocation = loc
                        locationPoints = locationPoints + loc
                    }
                }
            }

            try {
                activity.fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
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

    // ── Formatters ────────────────────────────────────────────────────────────
    val formattedTime = remember(durationSeconds) {
        val hrs = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60
        if (hrs > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
        else String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val formattedDistance = remember(distanceMeters) {
        String.format(Locale.getDefault(), "%.2f", distanceMeters / 1000f)
    }

    val pace = remember(durationSeconds, distanceMeters) {
        if (distanceMeters < 100) "--:--"
        else {
            val km = distanceMeters / 1000f
            val minutesPerKm = (durationSeconds / 60f) / km
            val m = minutesPerKm.toInt()
            val s = ((minutesPerKm - m) * 60).toInt()
            String.format(Locale.getDefault(), "%d:%02d", m, s)
        }
    }

    // ── Calorie calculation + saveActivity ────────────────────────────────────
    fun calculateCalories(): Int {
        val km = distanceMeters / 1000f
        val caloriesPerKm = when (activityType) {
            "Running" -> 60
            "Cycling" -> 30
            "Walking" -> 40
            "Hiking" -> 50
            else -> 50
        }
        return (km * caloriesPerKm).toInt()
    }

    fun saveActivity() {
        isSaving = true
        val calories = calculateCalories()
        val request = ActivityRequest(
            user_id = userId,
            activity_type = activityType,
            duration = durationSeconds.toInt() / 60,
            distance = distanceMeters / 1000.0,
            calories = calories,
            notes = "Tracked via GPS"
        )
        ApiClient.apiService.saveActivity(request)
            .enqueue(object : Callback<ActivityResponse> {
                override fun onResponse(
                    call: Call<ActivityResponse>,
                    response: Response<ActivityResponse>
                ) {
                    isSaving = false
                    val body = response.body()
                    if (body != null && body.success) {
                        Toast.makeText(context, "Activity saved!", Toast.LENGTH_SHORT).show()
                        onFinish(distanceMeters, durationSeconds, calories)
                    } else {
                        Toast.makeText(
                            context,
                            body?.message ?: "Failed to save",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ActivityResponse>, t: Throwable) {
                    isSaving = false
                    val msg = when (t) {
                        is java.net.ConnectException -> "Cannot connect to server. Check your network."
                        is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                        else -> "Error: ${t.localizedMessage ?: "Unknown error"}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            })
    }

    // ── Activity icon ─────────────────────────────────────────────────────────
    val activityIcon = when (activityType) {
        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
        "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Hiking" -> Icons.Default.Terrain
        else -> Icons.Default.Favorite
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        TrackingHeader(
            activityType = activityType,
            activityIcon = activityIcon,
            onBack = onBack
        )

        // ── Permission denied ─────────────────────────────────────────────────
        if (!hasPermission) {
            PermissionDeniedView()
        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Animation Section ──────────────────────────────────────────
                AnimationSection(
                    activityType = activityType,
                    trackingState = trackingState,
                    locationPoints = locationPoints,
                    isDark = isDark
                )

                // ── Timer Display ─────────────────────────────────────────────
                TimerDisplay(
                    formattedTime = formattedTime,
                    trackingState = trackingState
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Stats Row ─────────────────────────────────────────────────
                TrackingStatsOverlay(
                    formattedDistance = formattedDistance,
                    pace = pace,
                    currentSpeed = currentSpeed,
                    surfaceColor = surfaceColor
                )

                Spacer(modifier = Modifier.weight(1f))

                // ── Control Buttons ────────────────────────────────────────────
                TrackingControls(
                    trackingState = trackingState,
                    activityType = activityType,
                    isSaving = isSaving,
                    onPause = { trackingState = TrackingState.PAUSED },
                    onResume = { trackingState = TrackingState.RUNNING },
                    onStart = { trackingState = TrackingState.RUNNING },
                    onFinish = {
                        trackingState = TrackingState.STOPPED
                        saveActivity()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable UI Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrackingHeader(
    activityType: String,
    activityIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Icon(
            imageVector = activityIcon,
            contentDescription = activityType,
            tint = MaterialTheme.colorScheme.primary,
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
}

@Composable
private fun PermissionDeniedView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.LocationOff,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Location permission required",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp
            )
            Text(
                "Please grant location permission to track your activity",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AnimationSection(
    activityType: String,
    trackingState: TrackingState,
    locationPoints: List<android.location.Location>,
    isDark: Boolean
) {
    when (activityType) {
        "Running", "Walking" -> {
            ActivityAnimationScene(
                activityType = activityType,
                trackingState = trackingState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        "Cycling" -> {
            CyclingMapView(
                locationPoints = locationPoints,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        "Hiking" -> {
            HikingAnimationScene(
                trackingState = trackingState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        else -> {
            DefaultAnimationScene(
                trackingState = trackingState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TimerDisplay(
    formattedTime: String,
    trackingState: TrackingState
) {
    Text(
        text = formattedTime,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = when (trackingState) {
            TrackingState.IDLE -> "Ready"
            TrackingState.RUNNING -> "In Progress"
            TrackingState.PAUSED -> "⏸  Paused"
            TrackingState.STOPPED -> "Saving…"
        },
        color = when (trackingState) {
            TrackingState.RUNNING -> MaterialTheme.colorScheme.secondary
            TrackingState.PAUSED -> MaterialTheme.colorScheme.tertiary
            else -> Color.Gray
        },
        fontSize = 16.sp
    )
}

@Composable
private fun TrackingStatsOverlay(
    formattedDistance: String,
    pace: String,
    currentSpeed: Float,
    surfaceColor: Color
) {
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
            value = String.format(Locale.getDefault(), "%.1f", currentSpeed * 3.6f),
            unit = "km/h",
            cardBg = surfaceColor
        )
    }
}

@Composable
private fun TrackingControls(
    trackingState: TrackingState,
    activityType: String,
    isSaving: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    when (trackingState) {

        // START — single wide button
        TrackingState.IDLE -> {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
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
                    "START $activityType".uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // RUNNING — Pause | Finish
        TrackingState.RUNNING -> {
            ControlButtonRow(
                leftLabel = "PAUSE",
                leftIcon = Icons.Default.Pause,
                leftColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                leftTextColor = MaterialTheme.colorScheme.onSurface,
                leftOnClick = onPause,
                rightLabel = "FINISH",
                rightIcon = Icons.Default.Stop,
                rightColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                rightEnabled = !isSaving,
                isSaving = isSaving,
                rightOnClick = onFinish
            )
        }

        // PAUSED — Resume | Finish
        TrackingState.PAUSED -> {
            ControlButtonRow(
                leftLabel = "RESUME",
                leftIcon = Icons.Default.PlayArrow,
                leftColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                leftTextColor = Color.White,
                leftOnClick = onResume,
                rightLabel = "FINISH",
                rightIcon = Icons.Default.Stop,
                rightColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                rightEnabled = !isSaving,
                isSaving = isSaving,
                rightOnClick = onFinish
            )
        }

        // STOPPED — saving spinner
        TrackingState.STOPPED -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Saving…",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ControlButtonRow(
    leftLabel: String,
    leftIcon: androidx.compose.ui.graphics.vector.ImageVector,
    leftColors: ButtonColors,
    leftTextColor: Color,
    leftOnClick: () -> Unit,
    rightLabel: String,
    rightIcon: androidx.compose.ui.graphics.vector.ImageVector,
    rightColors: ButtonColors,
    rightEnabled: Boolean,
    isSaving: Boolean,
    rightOnClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = leftOnClick,
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
            colors = leftColors,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                leftIcon,
                contentDescription = leftLabel,
                modifier = Modifier.size(28.dp),
                tint = leftTextColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                leftLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = leftTextColor
            )
        }

        Button(
            onClick = rightOnClick,
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
            colors = rightColors,
            shape = RoundedCornerShape(16.dp),
            enabled = rightEnabled
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    rightIcon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    rightLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CyclingMapView - MapLibre Implementation
// ─────────────────────────────────────────────────────────────────────────────
private const val PATH_SOURCE_ID = "cycling-path-source"
private const val PATH_LAYER_ID = "cycling-path-layer"
private const val POS_SOURCE_ID = "cycling-pos-source"
private const val POS_LAYER_ID = "cycling-pos-layer"

// Updated with your exact MapTiler API Key
private const val MAPTILER_KEY = "5SWQsfM09VXwlm3ZE3Im"
private const val MAP_STYLE_URL = "https://api.maptiler.com/maps/streets-v2/style.json?key=$MAPTILER_KEY"

@Composable
fun CyclingMapView(
    locationPoints: List<android.location.Location>,
    isDark: Boolean, // Kept for parameter consistency, though streets-v2 is used
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Initialize MapLibre SDK once
    remember { Mapbox.getInstance(context) }

    val mapboxMapRef = remember { mutableStateOf<MapboxMap?>(null) }
    val mapView = remember { MapView(context) }

    // ── Perfect Lifecycle Handling ────────────────────────────────────────────
    // This forwards every Activity/Fragment lifecycle event 1-to-1 to the MapView
    // so the GL surface, state, and sensors are created/destroyed at the exact right time.
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_CREATE  -> mapView.onCreate(null)
                androidx.lifecycle.Lifecycle.Event.ON_START   -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP    -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> { /* ON_ANY ignored */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Clean up observer when the Composable leaves the composition
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── Map Setup & Style ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapboxMapRef.value = map
            map.cameraPosition = CameraPosition.Builder().zoom(15.0).build()

            // Apply your locked streets-v2 style URL
            map.setStyle(MAP_STYLE_URL) { style ->
                val emptyGeoJson = """{ "type": "FeatureCollection", "features": [] }"""

                // Add Route Polyline Source & Layer
                style.addSource(GeoJsonSource(PATH_SOURCE_ID, FeatureCollection.fromJson(emptyGeoJson)))
                style.addLayer(
                    LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID).apply {
                        setProperties(
                            PropertyFactory.lineColor(android.graphics.Color.rgb(220, 50, 50)), // Vital Red
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    }
                )

                // Add Current Position Dot Source & Layer
                style.addSource(GeoJsonSource(POS_SOURCE_ID, FeatureCollection.fromJson(emptyGeoJson)))
                style.addLayer(
                    CircleLayer(POS_LAYER_ID, POS_SOURCE_ID).apply {
                        setProperties(
                            PropertyFactory.circleRadius(10f),
                            PropertyFactory.circleColor(android.graphics.Color.WHITE),
                            PropertyFactory.circleStrokeWidth(3f),
                            PropertyFactory.circleStrokeColor(android.graphics.Color.rgb(220, 50, 50))
                        )
                    }
                )

                // Seed camera to last known GPS location so it doesn't open on the ocean
                try {
                    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 15.0
                                    )
                                )
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w("CyclingMap", "lastLocation unavailable: ${e.message}")
                }
            }
        }
    }

    // ── Live Polyline & Marker Updates ────────────────────────────────────────
    LaunchedEffect(locationPoints) {
        val map = mapboxMapRef.value ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (locationPoints.isEmpty()) return@LaunchedEffect

        // Update the red line tracking the route
        val coordinates = locationPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
        (style.getSource(PATH_SOURCE_ID) as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                listOf(Feature.fromGeometry(LineString.fromLngLats(coordinates)))
            )
        )

        // Update the white dot to the latest GPS ping
        val last = locationPoints.last()
        (style.getSource(POS_SOURCE_ID) as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                listOf(Feature.fromGeometry(Point.fromLngLat(last.longitude, last.latitude)))
            )
        )

        // Smoothly pan the camera to follow the cyclist
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(last.latitude, last.longitude), 16.0
            )
        )
    }

    // ── Render MapView ────────────────────────────────────────────────────────
    AndroidView(
        factory = { mapView },
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ActivityAnimationScene - Running & Walking
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParallaxBackground(
    scrollOffset: Float,
    isDark: Boolean
) {
    val skyTop = if (isDark) Color(0xFF0D1B2A) else Color(0xFF87CEEB)
    val skyBottom = if (isDark) Color(0xFF1B2838) else Color(0xFFD4EEFF)
    val roadColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFF4A4A4A)
    val dashColor = Color(0xFFFFDD44)
    val grassColor = if (isDark) Color(0xFF1A3A1A) else Color(0xFF5DBB63)
    val trunkColor = if (isDark) Color(0xFF5C3D1E) else Color(0xFF8B5E3C)
    val leafColor = if (isDark) Color(0xFF2D5A2D) else Color(0xFF228B22)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        val w = size.width
        val h = size.height
        val dp = density

        val tileWidthPx = 240f * dp
        val scrollPx = (scrollOffset * dp) % tileWidthPx

        // Sky
        val skyH = h * 0.55f
        drawRect(
            brush = Brush.verticalGradient(listOf(skyTop, skyBottom), 0f, skyH),
            size = Size(w, skyH)
        )

        // Grass
        drawRect(
            color = grassColor,
            topLeft = Offset(0f, skyH),
            size = Size(w, h - skyH)
        )

        // Road
        val roadTop = h * 0.68f
        val roadH = h - roadTop
        drawRect(
            color = roadColor,
            topLeft = Offset(0f, roadTop),
            size = Size(w, roadH)
        )

        // Dashed center line
        val dashW = 40f * dp
        val gapW = 30f * dp
        val dashY = roadTop + roadH * 0.5f
        val dashHH = 3f * dp
        var dashX = -(scrollPx % (dashW + gapW))
        while (dashX < w) {
            drawRect(
                color = dashColor,
                topLeft = Offset(dashX, dashY - dashHH),
                size = Size(dashW, dashHH * 2)
            )
            dashX += dashW + gapW
        }

        // Trees
        val treeSlots = listOf(40f * dp, 160f * dp)
        val numTiles = (w / tileWidthPx).toInt() + 2
        for (tile in -1..numTiles) {
            val tileOriginX = tile * tileWidthPx - scrollPx
            for (treeOffX in treeSlots) {
                val tx = tileOriginX + treeOffX
                val trunkW = 8f * dp
                val trunkH = 28f * dp
                val trunkTop = skyH - trunkH
                val leafR = 20f * dp
                drawRect(
                    color = trunkColor,
                    topLeft = Offset(tx - trunkW / 2f, trunkTop),
                    size = Size(trunkW, trunkH)
                )
                drawCircle(
                    color = leafColor,
                    radius = leafR,
                    center = Offset(tx, trunkTop)
                )
            }
        }
    }
}

@Composable
private fun StickFigure(
    phase: Float,
    swingAmplitude: Float
) {
    val figureColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dp = density

        val roadTop = h * 0.68f
        val cx = w * 0.5f
        val footY = roadTop - 2f * dp
        val legLen = 28f * dp
        val torsoH = 30f * dp
        val armLen = 22f * dp
        val headR = 12f * dp
        val strokeW = 4f * dp

        val hipY = footY - legLen
        val shouldY = hipY - torsoH
        val headCY = shouldY - headR - 2f * dp

        val leftLegAngle = kotlin.math.sin(phase) * swingAmplitude
        val rightLegAngle = -kotlin.math.sin(phase) * swingAmplitude
        val leftArmAngle = -kotlin.math.sin(phase) * 0.7f * swingAmplitude
        val rightArmAngle = kotlin.math.sin(phase) * 0.7f * swingAmplitude

        fun drawLimb(ox: Float, oy: Float, angle: Float, length: Float) {
            val ex = ox + kotlin.math.sin(angle) * length
            val ey = oy + kotlin.math.cos(angle) * length
            drawLine(
                color = figureColor,
                start = Offset(ox, oy),
                end = Offset(ex, ey),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = figureColor,
            radius = headR,
            center = Offset(cx, headCY)
        )
        drawLine(
            color = figureColor,
            start = Offset(cx, shouldY),
            end = Offset(cx, hipY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLimb(cx, hipY, leftLegAngle, legLen)
        drawLimb(cx, hipY, rightLegAngle, legLen)
        drawLimb(cx, shouldY, leftArmAngle, armLen)
        drawLimb(cx, shouldY, rightArmAngle, armLen)
    }
}

@Composable
fun ActivityAnimationScene(
    activityType: String,
    trackingState: TrackingState,
    modifier: Modifier = Modifier
) {
    val scrollSpeedDp = when {
        trackingState == TrackingState.PAUSED ||
                trackingState == TrackingState.IDLE ||
                trackingState == TrackingState.STOPPED -> 0f
        activityType == "Running" -> 220f
        else -> 90f
    }
    val swingAmplitude = if (activityType == "Running") 0.72f else 0.38f
    val cycleDurationMs = if (activityType == "Running") 500 else 900

    val scrollOffset = remember { Animatable(0f) }
    LaunchedEffect(trackingState, activityType) {
        if (scrollSpeedDp != 0f) {
            while (true) {
                scrollOffset.animateTo(
                    targetValue = scrollOffset.value + scrollSpeedDp,
                    animationSpec = tween(1000, easing = LinearEasing)
                )
            }
        } else {
            scrollOffset.stop()
        }
    }

    val limbPhase = remember { Animatable(0f) }
    LaunchedEffect(trackingState, activityType) {
        if (scrollSpeedDp != 0f) {
            while (true) {
                limbPhase.animateTo(
                    targetValue = limbPhase.value + (2f * Math.PI.toFloat()),
                    animationSpec = tween(cycleDurationMs, easing = LinearEasing)
                )
            }
        } else {
            limbPhase.stop()
        }
    }

    Box(modifier = modifier) {
        ParallaxBackground(
            scrollOffset = scrollOffset.value,
            isDark = isSystemInDarkTheme()
        )
        StickFigure(
            phase = limbPhase.value,
            swingAmplitude = swingAmplitude
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HikingAnimationScene - Realistic Fuji Mountain View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HikingAnimationScene(
    trackingState: TrackingState,
    modifier: Modifier = Modifier
) {
    val isMoving = trackingState == TrackingState.RUNNING

    // Ground scroll — 35 dp/s downward
    val scrollOffset = remember { Animatable(0f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            while (true) {
                scrollOffset.animateTo(
                    targetValue = scrollOffset.value + 35f,
                    animationSpec = tween(1_000, easing = LinearEasing)
                )
            }
        } else {
            scrollOffset.stop()
        }
    }

    // Cloud drift — slow horizontal
    val cloudDrift = remember { Animatable(0f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            while (true) {
                cloudDrift.animateTo(
                    targetValue = cloudDrift.value + 15f,
                    animationSpec = tween(1_000, easing = LinearEasing)
                )
            }
        } else {
            cloudDrift.stop()
        }
    }

    // Mountain scale — 1.0 → 1.12 over 60 s
    val mountainScale = remember { Animatable(1.0f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            mountainScale.animateTo(
                targetValue = 1.12f,
                animationSpec = tween(60_000, easing = LinearEasing)
            )
        } else {
            mountainScale.stop()
        }
    }

    // Wind line phase
    val windPhase = remember { Animatable(0f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            while (true) {
                windPhase.animateTo(
                    targetValue = windPhase.value + (2f * Math.PI.toFloat()),
                    animationSpec = tween(3_000, easing = LinearEasing)
                )
            }
        } else {
            windPhase.stop()
        }
    }

    // Hiker limb cycle — 2.2 s per stroke
    val limbPhase = remember { Animatable(0f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            while (true) {
                limbPhase.animateTo(
                    targetValue = limbPhase.value + (2f * Math.PI.toFloat()),
                    animationSpec = tween(2_200, easing = LinearEasing)
                )
            }
        } else {
            limbPhase.stop()
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        HikingBackgroundCanvas(
            scrollOffset = scrollOffset.value,
            cloudDrift = cloudDrift.value,
            mountainScale = mountainScale.value,
            windPhase = windPhase.value,
            isDark = isSystemInDarkTheme()
        )
        HikingFigureCanvas(
            phase = limbPhase.value,
            isDark = isSystemInDarkTheme()
        )
    }
}

@Composable
private fun HikingBackgroundCanvas(
    scrollOffset: Float,
    cloudDrift: Float,
    mountainScale: Float,
    windPhase: Float,
    isDark: Boolean
) {
    val skyTop = if (isDark) Color(0xFF0A1628) else Color(0xFF87CEEB)
    val skyBot = if (isDark) Color(0xFF1A2A1A) else Color(0xFFFFFFFF)
    val cloudCol = Color.White.copy(alpha = if (isDark) 0.20f else 0.82f)
    val mtnBody = if (isDark) Color(0xFF1B5E20) else Color(0xFF2E7D32)
    val mtnShadow = if (isDark) Color(0xFF0D3B12) else Color(0xFF1B5E20)
    val treeCanopy = if (isDark) Color(0xFF0A3A0A) else Color(0xFF1B5E20)
    val treeTrunk = if (isDark) Color(0xFF3E2723) else Color(0xFF5D4037)
    val trailCol = if (isDark) Color(0xFF5D4037) else Color(0xFF8D6E63)
    val groundEarth = if (isDark) Color(0xFF3E2723) else Color(0xFF8D6E63)
    val groundDarker = if (isDark) Color(0xFF2E1A0E) else Color(0xFF6D4C41)
    val rockCol = if (isDark) Color(0xFF4E342E) else Color(0xFF795548)
    val windCol = Color.White.copy(alpha = if (isDark) 0.10f else 0.30f)
    val fgTreeCanopy = if (isDark) Color(0xFF2E7D32) else Color(0xFF388E3C)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dp = density
        val scrollPx = scrollOffset * dp
        val driftPx = cloudDrift * dp
        val sc = mountainScale

        // Layer 0: Sky gradient
        drawRect(Brush.verticalGradient(listOf(skyTop, skyBot), 0f, h), size = size)

        // Layer 1: Clouds
        val tileW = w * 1.6f
        val cShift = driftPx % tileW
        data class Cld(val x: Float, val y: Float, val s: Float)
        listOf(
            Cld(0.08f, 0.05f, 1.0f), Cld(0.42f, 0.08f, 0.78f),
            Cld(0.72f, 0.03f, 0.92f), Cld(1.10f, 0.12f, 0.65f),
            Cld(0.28f, 0.18f, 0.58f)
        ).forEach { c ->
            for (tile in -1..1) {
                val cx = c.x * w + tile * tileW - cShift
                val cy = c.y * h
                if (cx > -70f * dp && cx < w + 70f * dp) {
                    val cw = 40f * dp * c.s
                    val ch = 9f * dp * c.s
                    drawOval(cloudCol, Offset(cx, cy), Size(cw, ch))
                    drawOval(
                        cloudCol,
                        Offset(cx + cw * 0.18f, cy - ch * 0.65f),
                        Size(cw * 0.55f, ch * 1.3f)
                    )
                    drawOval(
                        cloudCol,
                        Offset(cx + cw * 0.42f, cy - ch * 0.35f),
                        Size(cw * 0.45f, ch * 1.1f)
                    )
                }
            }
        }

        // Layer 2: Fuji-shaped mountain
        val mtnCx = w * 0.48f
        val mtnBaseY = h * 0.70f
        val mtnPeakY = h * 0.06f
        val mtnHalfW = w * 0.44f

        // Left slope (darker shadow)
        val sp = Path()
        sp.moveTo(mtnCx, mtnPeakY * (2f - sc))
        sp.cubicTo(
            mtnCx - mtnHalfW * 0.32f * sc,
            mtnPeakY + (mtnBaseY - mtnPeakY) * 0.28f,
            mtnCx - mtnHalfW * 0.82f * sc,
            mtnPeakY + (mtnBaseY - mtnPeakY) * 0.68f,
            mtnCx - mtnHalfW * sc,
            mtnBaseY
        )
        sp.lineTo(mtnCx, mtnBaseY)
        sp.close()
        drawPath(sp, mtnShadow)

        // Right slope (brighter body)
        val mp = Path()
        mp.moveTo(mtnCx, mtnPeakY * (2f - sc))
        mp.cubicTo(
            mtnCx + mtnHalfW * 0.30f * sc,
            mtnPeakY + (mtnBaseY - mtnPeakY) * 0.24f,
            mtnCx + mtnHalfW * 0.78f * sc,
            mtnPeakY + (mtnBaseY - mtnPeakY) * 0.62f,
            mtnCx + mtnHalfW * sc,
            mtnBaseY
        )
        mp.lineTo(mtnCx, mtnBaseY)
        mp.close()
        drawPath(mp, mtnBody)

        // Layer 2b: Trees on mountain face
        data class MtnTree(val xOff: Float, val yFrac: Float, val r: Float)
        listOf(
            MtnTree(-0.20f, 0.42f, 4.5f), MtnTree(-0.10f, 0.50f, 5.0f),
            MtnTree(0.05f, 0.38f, 3.8f), MtnTree(0.15f, 0.55f, 5.5f),
            MtnTree(-0.28f, 0.58f, 4.0f), MtnTree(0.25f, 0.48f, 4.2f),
            MtnTree(-0.05f, 0.62f, 5.8f), MtnTree(0.10f, 0.65f, 4.8f),
            MtnTree(-0.18f, 0.35f, 3.5f), MtnTree(0.20f, 0.42f, 4.0f),
            MtnTree(-0.30f, 0.65f, 5.2f), MtnTree(0.30f, 0.60f, 4.5f),
            MtnTree(0.00f, 0.50f, 5.0f), MtnTree(-0.12f, 0.72f, 6.0f),
            MtnTree(0.08f, 0.72f, 5.5f), MtnTree(-0.24f, 0.48f, 3.6f),
            MtnTree(0.18f, 0.35f, 3.2f), MtnTree(-0.06f, 0.28f, 3.0f),
        ).forEach { t ->
            val tx = mtnCx + t.xOff * mtnHalfW * sc
            val ty = mtnPeakY + (mtnBaseY - mtnPeakY) * t.yFrac
            val tr = t.r * dp
            drawLine(
                treeTrunk,
                Offset(tx, ty + tr * 0.3f),
                Offset(tx, ty + tr * 1.2f),
                1.5f * dp,
                cap = StrokeCap.Round
            )
            drawCircle(treeCanopy, tr, Offset(tx, ty))
        }

        // Layer 2c: Zigzag trail
        val trailSw = 2.2f * dp
        val segs = 7
        for (i in 0 until segs) {
            val t0 = i.toFloat() / segs
            val t1 = (i + 1).toFloat() / segs
            val y0 = mtnPeakY + (mtnBaseY - mtnPeakY) * (0.25f + t0 * 0.70f)
            val y1 = mtnPeakY + (mtnBaseY - mtnPeakY) * (0.25f + t1 * 0.70f)
            val x0 = mtnCx + (if (i % 2 == 0) 1f else -1f) * mtnHalfW * sc * (0.08f + t0 * 0.22f)
            val x1 = mtnCx + (if (i % 2 == 0) -1f else 1f) * mtnHalfW * sc * (0.08f + t1 * 0.22f)
            drawLine(trailCol, Offset(x0, y0), Offset(x1, y1), trailSw, cap = StrokeCap.Round)
        }

        // Layer 3: Wind lines
        data class Wind(val xF: Float, val yF: Float, val len: Float, val phaseOff: Float)
        listOf(
            Wind(0.22f, 0.30f, 0.18f, 0.0f),
            Wind(0.55f, 0.38f, 0.15f, 1.2f),
            Wind(0.35f, 0.50f, 0.20f, 2.5f),
            Wind(0.60f, 0.55f, 0.14f, 3.8f),
            Wind(0.28f, 0.65f, 0.16f, 5.0f),
        ).forEach { wl ->
            val alpha = (kotlin.math.sin(windPhase + wl.phaseOff).toFloat() * 0.5f + 0.5f) * 0.35f
            val wx = wl.xF * w
            val wy = wl.yF * h
            drawLine(
                windCol.copy(alpha = alpha),
                Offset(wx, wy),
                Offset(wx + wl.len * w, wy - 2f * dp),
                strokeWidth = 1.2f * dp,
                cap = StrokeCap.Round
            )
        }

        // Layer 4: Ground
        val groundTop = h * 0.74f
        val gScrollPx = (scrollPx * 0.6f) % (28f * dp)

        val gp = Path()
        gp.moveTo(0f, groundTop)
        gp.cubicTo(
            w * 0.15f, groundTop - 6f * dp, w * 0.30f, groundTop + 4f * dp,
            w * 0.50f, groundTop - 3f * dp
        )
        gp.cubicTo(
            w * 0.70f, groundTop + 5f * dp, w * 0.85f, groundTop - 4f * dp,
            w, groundTop + 2f * dp
        )
        gp.lineTo(w, h)
        gp.lineTo(0f, h)
        gp.close()
        drawPath(gp, groundEarth)

        // Scrolling texture lines
        for (i in 0..4) {
            val ly = groundTop + 8f * dp + i * 6f * dp + gScrollPx
            if (ly < h) {
                drawLine(
                    groundDarker,
                    Offset(w * 0.05f, ly),
                    Offset(w * 0.95f, ly),
                    1f * dp,
                    cap = StrokeCap.Round
                )
            }
        }

        // Layer 4b: Rocks
        data class Rock(val xF: Float, val wF: Float, val hF: Float)
        listOf(
            Rock(0.08f, 0.06f, 0.03f), Rock(0.30f, 0.04f, 0.025f),
            Rock(0.55f, 0.05f, 0.028f), Rock(0.78f, 0.07f, 0.032f),
            Rock(0.92f, 0.04f, 0.022f), Rock(0.18f, 0.035f, 0.020f),
        ).forEach { r ->
            val rx = r.xF * w
            val ry = groundTop + 2f * dp
            drawOval(rockCol, Offset(rx, ry), Size(r.wF * w, r.hF * h))
        }

        // Layer 4c: Foreground trees
        data class FgTree(val xF: Float, val trH: Float, val canR: Float)
        listOf(
            FgTree(0.06f, 18f, 8f), FgTree(0.22f, 14f, 6.5f),
            FgTree(0.82f, 16f, 7f), FgTree(0.94f, 12f, 5.5f),
        ).forEach { t ->
            val tx = t.xF * w
            val tBase = groundTop
            val trH = t.trH * dp
            drawLine(
                treeTrunk,
                Offset(tx, tBase),
                Offset(tx, tBase - trH),
                2f * dp,
                cap = StrokeCap.Round
            )
            drawCircle(
                fgTreeCanopy,
                t.canR * dp,
                Offset(tx, tBase - trH - t.canR * dp * 0.5f)
            )
        }
    }
}

@Composable
private fun HikingFigureCanvas(
    phase: Float,
    isDark: Boolean
) {
    val bodyColor = if (isDark) Color(0xFFF5E680) else Color(0xFFFFFFFF)
    val poleColor = if (isDark) Color(0xFFBFA96A) else Color(0xFFBCAAA4)
    val packColor = if (isDark) Color(0xFF8D6E63) else Color(0xFFEF5350)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dp = density

        val hikerCx = w * 0.68f
        val hikerGround = h * 0.66f

        val headR = 2.2f * dp
        val torsoLen = 6.5f * dp
        val legLen = 5.5f * dp
        val armLen = 4f * dp
        val sw = 1.4f * dp

        val lean = 0.65f

        val hipX = hikerCx
        val hipY = hikerGround - legLen * 0.45f
        val shouldX = hipX - kotlin.math.sin(lean).toFloat() * torsoLen
        val shouldY = hipY - kotlin.math.cos(lean).toFloat() * torsoLen
        val headCX = shouldX - kotlin.math.sin(lean).toFloat() * (headR + 0.8f * dp)
        val headCY = shouldY - kotlin.math.cos(lean).toFloat() * (headR + 0.8f * dp)

        val swing = 0.32f
        val lLeg = kotlin.math.sin(phase).toFloat() * swing
        val rLeg = -kotlin.math.sin(phase).toFloat() * swing
        val lArm = -kotlin.math.sin(phase).toFloat() * swing * 0.8f
        val rArm = kotlin.math.sin(phase).toFloat() * swing * 0.8f

        fun limb(ox: Float, oy: Float, angle: Float, len: Float) {
            drawLine(
                bodyColor,
                Offset(ox, oy),
                Offset(
                    ox + kotlin.math.sin(angle).toFloat() * len,
                    oy + kotlin.math.cos(angle).toFloat() * len
                ),
                sw, cap = StrokeCap.Round
            )
        }

        drawCircle(bodyColor, headR, Offset(headCX, headCY))

        val pkCx = (shouldX + hipX) * 0.5f + 1.2f * dp
        val pkCy = (shouldY + hipY) * 0.5f
        drawRect(
            packColor,
            Offset(pkCx - 1.5f * dp, pkCy - 2f * dp),
            Size(3f * dp, 4f * dp)
        )

        drawLine(
            bodyColor,
            Offset(shouldX, shouldY),
            Offset(hipX, hipY),
            sw,
            cap = StrokeCap.Round
        )

        limb(hipX, hipY, lLeg, legLen)
        limb(hipX, hipY, rLeg, legLen)
        limb(shouldX, shouldY, lArm, armLen)
        limb(shouldX, shouldY, rArm, armLen)

        val ptX = shouldX + kotlin.math.sin(rArm).toFloat() * armLen
        val ptY = shouldY + kotlin.math.cos(rArm).toFloat() * armLen
        drawLine(
            poleColor,
            Offset(ptX, ptY),
            Offset(ptX + 2.5f * dp, hikerGround + 1f * dp),
            sw * 0.5f,
            cap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DefaultAnimationScene - Fallback for other activity types
// ─────────────────────────────────────────────────────────────────────────────


@Composable
fun DefaultAnimationScene(
    trackingState: TrackingState,
    modifier: Modifier = Modifier
) {
    val isMoving = trackingState == TrackingState.RUNNING
    val isDark = isSystemInDarkTheme()

    // FIX: Captured outside Canvas because MaterialTheme.colorScheme is @Composable
    val accentColor = MaterialTheme.colorScheme.primary

    val scrollOffset = remember { Animatable(0f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            while (true) {
                scrollOffset.animateTo(
                    targetValue = scrollOffset.value + 100f,
                    animationSpec = tween(1_000, easing = LinearEasing)
                )
            }
        } else {
            scrollOffset.stop()
        }
    }

    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isMoving) {
        if (isMoving) {
            while (true) {
                pulseScale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(800, easing = LinearEasing)
                )
                pulseScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(800, easing = LinearEasing)
                )
            }
        } else {
            pulseScale.stop()
        }
    }

    Canvas(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        val w = size.width
        val h = size.height
        val dp = density
        val scrollPx = scrollOffset.value * dp

        // Now it safely uses the variable captured above
        val bgColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFE3F2FD)
        drawRect(bgColor, size = size)

        val circleSpacing = 60f * dp
        val numCircles = (w / circleSpacing + 2).toInt()
        for (i in 0..numCircles) {
            val cx = i * circleSpacing - (scrollPx % circleSpacing)
            val cy = h * 0.5f + kotlin.math.sin((i + scrollOffset.value / 50f) * 0.5f) * 30f * dp
            val radius = (15f + i * 2f) * dp * pulseScale.value
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(cx, cy)
            )
        }

        drawCircle(
            color = accentColor.copy(alpha = 0.2f),
            radius = 40f * dp * pulseScale.value,
            center = Offset(w / 2, h / 2)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.4f),
            radius = 25f * dp * pulseScale.value,
            center = Offset(w / 2, h / 2)
        )
    }
}