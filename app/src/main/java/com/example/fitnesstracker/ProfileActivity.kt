package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            ProfileScreenContent(
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                onBack = { finish() },
                onLogout = {
                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            )
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
    var activities by remember { mutableStateOf<List<ActivityData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val darkPurple = Color(0xFF1A0A2E)
    val purple = Color(0xFF6B4C9A)
    val lightPurple = Color(0xFF9B7DD4)
    val cardBg = Color(0xFF2D1B4E)
    val accent = Color(0xFFE94560)

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
            .background(darkPurple)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(purple, darkPurple)
                    )
                )
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
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
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = userEmail,
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
                cardBg = cardBg,
                accent = accent
            )
            ProfileStatCard(
                title = "Calories",
                value = totalCalories.toString(),
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                accent = accent
            )
            ProfileStatCard(
                title = "Minutes",
                value = totalDuration.toString(),
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                accent = accent
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
                    color = Color.White
                )
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    cardBg = cardBg,
                    purple = purple,
                    lightPurple = lightPurple
                ) {
                    // TODO: Navigate to Edit Profile
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage your notifications",
                    cardBg = cardBg,
                    purple = purple,
                    lightPurple = lightPurple
                ) {
                    // TODO: Navigate to Notifications
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy",
                    subtitle = "Manage your privacy settings",
                    cardBg = cardBg,
                    purple = purple,
                    lightPurple = lightPurple
                ) {
                    // TODO: Navigate to Privacy
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    cardBg = cardBg,
                    purple = purple,
                    lightPurple = lightPurple
                ) {
                    // TODO: Navigate to Help
                }
            }

            item {
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "App version and information",
                    cardBg = cardBg,
                    purple = purple,
                    lightPurple = lightPurple
                ) {
                    // TODO: Navigate to About
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Logout Button
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
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
                color = accent
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
                    color = Color.White
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