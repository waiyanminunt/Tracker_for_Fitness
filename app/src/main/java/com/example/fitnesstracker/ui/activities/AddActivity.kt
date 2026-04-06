package com.example.fitnesstracker.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityRequest
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme
import com.example.fitnesstracker.ui.viewmodel.FitnessViewModel
import com.example.fitnesstracker.ui.viewmodel.ViewModelFactory
import com.example.fitnesstracker.data.models.CardioWorkout
import com.example.fitnesstracker.data.models.StrengthWorkout
import com.example.fitnesstracker.utils.NotificationHelper

class AddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("FitnessTrackerPrefs", android.content.Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("USER_ID", 0)

        val repository = DataRepository(ApiClient.apiService)
        val factory = ViewModelFactory(application, repository)

        setContent {
            FitnesstrackerTheme {
                val viewModel: FitnessViewModel = viewModel(factory = factory)
                AddActivityScreen(
                    userId = userId,
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AddActivityScreen(
    userId: Int,
    viewModel: FitnessViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedActivity by remember { mutableStateOf<String?>(null) }
    val gpsActivities = listOf("Running", "Cycling", "Walking", "Hiking")

    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(selectedActivity) {
        if (selectedActivity != null && selectedActivity in gpsActivities) {
            val intent = Intent(context, TrackingActivity::class.java)
            intent.putExtra("ACTIVITY_TYPE", selectedActivity)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
            selectedActivity = null
        }
    }

    // Task 4: UI Overlap Fix
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedActivity != null) selectedActivity = null else onBack()
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
            ActivitySelectionGrid { selectedActivity = it }
        } else {
            ActivityInputForm(
                userId = userId,
                activityType = selectedActivity!!,
                viewModel = viewModel,
                onSave = {
                    selectedActivity = null
                }
            )
        }
    }
}

@Composable
fun ActivitySelectionGrid(onActivitySelected: (String) -> Unit) {
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
    val gpsActivities = listOf("Cycling", "Hiking", "Running", "Walking")

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp)
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

@Composable
fun ActivityCardGrid(name: String, icon: ImageVector, color: Color, hasGps: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (hasGps) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "GPS", tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "GPS Tracking", color = Color(0xFF4CAF50), fontSize = 11.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ActivityInputForm(userId: Int, activityType: String, viewModel: FitnessViewModel, onSave: () -> Unit) {
    val context = LocalContext.current
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val saveResult by viewModel.saveResult.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val primaryColor = MaterialTheme.colorScheme.primary
    val noDistanceActivities = listOf("Yoga", "Weightlifting", "Football")

    val calculatedCalories = remember(duration, distance, activityType) {
        val dMin = duration.toIntOrNull() ?: 0
        val dKm = distance.toDoubleOrNull() ?: 0.0
        when (activityType) {
            "Swimming" -> CardioWorkout(0, "Swimming", dMin, dKm).calculateCalories()
            "Weightlifting" -> StrengthWorkout(0, "Weightlifting", dMin, 3, 10).calculateCalories()
            "Yoga" -> ((2.8 * 3.5 * 70.0 / 200.0) * dMin).toInt()
            "Football" -> ((7.0 * 3.5 * 70.0 / 200.0) * dMin).toInt()
            else -> 0
        }
    }

    LaunchedEffect(saveResult) {
        saveResult?.onSuccess {
            // Post to Notifications inbox so Notifications screen shows real entries.
            NotificationHelper(context).addNotification(
                title = "✅ $activityType Saved!",
                message = "Your $activityType session has been recorded successfully.",
                type = "workout_saved"
            )
            Toast.makeText(context, "Activity saved!", Toast.LENGTH_SHORT).show()
            onSave()
            viewModel.clearResults()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearResults()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        if (duration.isNotEmpty() && calculatedCalories > 0) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Auto-Calculated Calories", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "$calculatedCalories kcal", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (min)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
        
        if (activityType !in noDistanceActivities) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = distance, onValueChange = { distance = it }, label = { Text("Distance (km)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (duration.isNotEmpty()) {
                    viewModel.saveActivity(ActivityRequest(userId, activityType, duration.toInt(), if (activityType in noDistanceActivities) 0.0 else distance.toDoubleOrNull() ?: 0.0, calculatedCalories, notes))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("SAVE ACTIVITY", fontWeight = FontWeight.ExtraBold)
        }
    }
}