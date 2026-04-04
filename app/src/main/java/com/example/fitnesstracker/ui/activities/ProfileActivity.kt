package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ActivitiesResponse
import com.example.fitnesstracker.data.network.ActivityData
import com.example.fitnesstracker.utils.BaseActivity
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import android.content.Intent
import android.os.Bundle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ============================================
// INHERITANCE EXAMPLE 2: ProfileActivity extends BaseActivity
// ============================================
// This class DEMONSTRATES INHERITANCE by:
// 1. Extending BaseActivity (inherits helper methods)
// 2. Using inherited methods: getUserId(), getUserName(), getUserEmail()
//
// OOP PRINCIPLE: INHERITANCE
// - Child class (ProfileActivity) extends Parent class (BaseActivity)
// - Inherits: getUserId(), getUserName(), getUserEmail()
// - Benefits: Code reuse, no need to repeat intent.getIntExtra() code
// ============================================

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // USING INHERITED METHODS from BaseActivity
        // This demonstrates INHERITANCE - we don't need to redefine these methods
        val userId = getUserId()
        val userName = getUserName()
        val userEmail = getUserEmail()

        setContent {
            FitnesstrackerTheme {
                ProfileScreenContent(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    onBack = { finish() },
                    onLogout = {
                        val sharedPrefs = getSharedPreferences("FitnessTrackerPrefs", android.content.Context.MODE_PRIVATE)
                        sharedPrefs.edit().clear().apply()
                        
                        val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreenContent(
    userId: Int,
    userName: String,
    userEmail: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var activities by remember { mutableStateOf<List<ActivityData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State for local user data that can be updated
    var currentName by remember { mutableStateOf(userName) }
    var currentEmail by remember { mutableStateOf(userEmail) }

    // Launcher for EditProfileActivity
    val editProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            currentName = data?.getStringExtra("UPDATED_NAME") ?: currentName
            currentEmail = data?.getStringExtra("UPDATED_EMAIL") ?: currentEmail
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Fetch activities for stats
    LaunchedEffect(userId) {
        ApiClient.apiService.getActivities(userId).enqueue(object : Callback<ActivitiesResponse> {
            override fun onResponse(
                call: Call<ActivitiesResponse>,
                response: Response<ActivitiesResponse>
            ) {
                isLoading = false
                val body = response.body()
                if (body != null && body.success) {
                    activities = body.activities
                }
            }

            override fun onFailure(call: Call<ActivitiesResponse>, t: Throwable) {
                isLoading = false
            }
        })
    }

    // Calculate stats
    val totalActivities = activities.size
    val totalCalories = activities.sumOf { it.calories }
    val totalDuration = activities.sumOf { it.duration }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primaryColor, backgroundColor)
                    )
                )
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Profile Info
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentName.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = currentEmail,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(
                title = "Activities",
                value = totalActivities.toString(),
                modifier = Modifier.weight(1f),
                cardBg = surfaceColor,
                accent = primaryColor
            )
            ProfileStatCard(
                title = "Calories",
                value = totalCalories.toString(),
                modifier = Modifier.weight(1f),
                cardBg = surfaceColor,
                accent = primaryColor
            )
            ProfileStatCard(
                title = "Minutes",
                value = totalDuration.toString(),
                modifier = Modifier.weight(1f),
                cardBg = surfaceColor,
                accent = primaryColor
            )
        }

        // Menu Items
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    cardBg = surfaceColor,
                    purple = primaryColor,
                    lightPurple = MaterialTheme.colorScheme.secondary
                ) {
                    val intent = Intent(context, EditProfileActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("USER_NAME", currentName)
                    intent.putExtra("USER_EMAIL", currentEmail)
                    editProfileLauncher.launch(intent)
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage your notifications",
                    cardBg = surfaceColor,
                    purple = primaryColor,
                    lightPurple = MaterialTheme.colorScheme.secondary
                ) {
                    val intent = Intent(context, NotificationsActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    context.startActivity(intent)
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy",
                    subtitle = "Manage your privacy settings",
                    cardBg = surfaceColor,
                    purple = primaryColor,
                    lightPurple = MaterialTheme.colorScheme.secondary
                ) {
                    // Update: Click listener to trigger the navigation Intent
                    val intent = Intent(context, PrivacyActivity::class.java)
                    context.startActivity(intent)
                }
            }




            item {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    cardBg = surfaceColor,
                    purple = primaryColor,
                    lightPurple = MaterialTheme.colorScheme.secondary
                ) {
                    val intent = Intent(context, HelpSupportActivity::class.java)
                    context.startActivity(intent)
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "App version and information",
                    cardBg = surfaceColor,
                    purple = primaryColor,
                    lightPurple = MaterialTheme.colorScheme.secondary
                ) {
                    val intent = Intent(context, AboutAppActivity::class.java)
                    context.startActivity(intent)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProfileStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    cardBg: Color,
    accent: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cardBg: Color,
    purple: Color,
    lightPurple: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(purple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = lightPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = Color.Gray
            )
        }
    }
}