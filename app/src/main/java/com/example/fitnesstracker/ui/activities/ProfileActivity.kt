package com.example.fitnesstracker.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.ChangePasswordRequest
import com.example.fitnesstracker.data.network.User
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme
import com.example.fitnesstracker.ui.viewmodel.FitnessViewModel
import com.example.fitnesstracker.ui.viewmodel.StatisticsViewModel
import com.example.fitnesstracker.ui.viewmodel.ViewModelFactory
import com.example.fitnesstracker.utils.ThemeManager

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("FitnessTrackerPrefs", android.content.Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("USER_ID", 0)

        val repository = DataRepository(ApiClient.apiService)
        val factory = ViewModelFactory(application, repository)

        setContent {
            var isDarkMode by remember { mutableStateOf(ThemeManager.isDarkMode.value) }
            FitnesstrackerTheme(darkTheme = isDarkMode) {
                val fitnessViewModel: FitnessViewModel = viewModel(factory = factory)
                val statsViewModel: StatisticsViewModel = viewModel(factory = factory)
                
                ProfileScreenContent(
                    userId = userId,
                    fitnessViewModel = fitnessViewModel,
                    statsViewModel = statsViewModel,
                    isDarkMode = isDarkMode,
                    onDarkModeToggle = { enabled ->
                        isDarkMode = enabled
                        ThemeManager.isDarkMode.value = enabled
                        sharedPrefs.edit().putBoolean("isDarkMode", enabled).apply()
                    },
                    onBack = { finish() },
                    onLogout = {
                        sharedPrefs.edit().clear().apply()
                        val intent = Intent(this, LoginActivity::class.java)
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
    fitnessViewModel: FitnessViewModel,
    statsViewModel: StatisticsViewModel,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by fitnessViewModel.userProfile.collectAsStateWithLifecycle()
    val activities by statsViewModel.activities.collectAsStateWithLifecycle()
    val isLoading by fitnessViewModel.isLoading.collectAsStateWithLifecycle()

    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val editProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            fitnessViewModel.fetchProfile(userId)
        }
    }

    LaunchedEffect(userId) {
        fitnessViewModel.fetchProfile(userId)
        statsViewModel.fetchActivities(userId)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            userId = userId,
            viewModel = fitnessViewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(primaryColor, backgroundColor)))
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier.align(Alignment.Center).padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        // Show first initial on success, '@' on failure
                        Text(
                            text = userProfile?.name?.take(1)?.uppercase() ?: "@",
                            fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Do NOT use 'Loading...' as a null-fallback — distinguish states explicitly
                Text(
                    text = when {
                        isLoading -> "Fetching profile…"
                        userProfile != null -> userProfile!!.name
                        else -> "Unknown User"
                    },
                    fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    text = userProfile?.email ?: "",
                    fontSize = 14.sp, color = Color.Gray
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard("Activities", activities.size.toString(), Modifier.weight(1f), surfaceColor, primaryColor)
            ProfileStatCard("Calories", activities.sumOf { it.calories }.toString(), Modifier.weight(1f), surfaceColor, primaryColor)
            ProfileStatCard("Minutes", activities.sumOf { it.duration }.toString(), Modifier.weight(1f), surfaceColor, primaryColor)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Account ──────────────────────────────────────────────────────
            item { SettingsSectionHeader("Account") }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    cardBg = surfaceColor,
                    purple = primaryColor
                ) {
                    val intent = Intent(context, EditProfileActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("USER_NAME", userProfile?.name)
                    intent.putExtra("USER_EMAIL", userProfile?.email)
                    editProfileLauncher.launch(intent)
                }
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your account password",
                    cardBg = surfaceColor,
                    purple = primaryColor
                ) { showChangePasswordDialog = true }
            }

            // ── Preferences ──────────────────────────────────────────────────
            item { SettingsSectionHeader("Preferences") }
            item {
                // Dark Mode — keeps its unique Switch layout
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = "Dark Mode",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            Text("Dark Mode", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (isDarkMode) "Dark theme active" else "Light theme active",
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onDarkModeToggle,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = primaryColor
                            )
                        )
                    }
                }
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage your activity alerts",
                    cardBg = surfaceColor,
                    purple = primaryColor
                ) { context.startActivity(Intent(context, NotificationsActivity::class.java)) }
            }

            // ── Privacy & Security ───────────────────────────────────────────
            item { SettingsSectionHeader("Privacy & Security") }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Security,
                    title = "Privacy Management",
                    subtitle = "Location, data sharing & permissions",
                    cardBg = surfaceColor,
                    purple = primaryColor
                ) { context.startActivity(Intent(context, PrivacyActivity::class.java)) }
            }

            // ── Support & Contact ────────────────────────────────────────────
            item { SettingsSectionHeader("Support & Contact") }
            item {
                // Contact Us — opens email client with pre-filled address
                ContactUsCard(surfaceColor = surfaceColor, primaryColor = primaryColor)
            }
            item {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help Center",
                    subtitle = "FAQ & User Guide",
                    cardBg = surfaceColor,
                    purple = primaryColor
                ) { context.startActivity(Intent(context, HelpSupportActivity::class.java)) }
            }

            // ── Information ──────────────────────────────────────────────────
            item { SettingsSectionHeader("Information") }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "About App",
                    subtitle = "Version 1.0.0  •  © 2026 Unt",
                    cardBg = surfaceColor,
                    purple = primaryColor
                ) { context.startActivity(Intent(context, AboutAppActivity::class.java)) }
            }

            // ── Logout ───────────────────────────────────────────────────────
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ProfileStatCard(title: String, value: String, modifier: Modifier, cardBg: Color, accent: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, cardBg: Color, purple: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(12.dp), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(purple.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, title, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, "Go", tint = Color.Gray)
        }
    }
}

/** Styled section label — separates logical groups in the settings list. */
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
    )
}

/**
 * Contact Us card — fires an email Intent with support@unt.com pre-filled.
 * Unlike ProfileMenuItem, it shows an email address subtitle instead of a chevron,
 * because this navigates out of the app rather than to another screen.
 */
@Composable
fun ContactUsCard(surfaceColor: Color, primaryColor: Color) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp),
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@unt.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Contact Us",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text("Contact Us", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("support@unt.com", fontSize = 12.sp, color = Color.Gray)
            }
            // External action — use send icon instead of chevron
            Icon(Icons.Default.Send, contentDescription = "Open Email", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun ChangePasswordDialog(userId: Int, viewModel: FitnessViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val passwordResult by viewModel.passwordResult.collectAsStateWithLifecycle()

    val primaryColor = MaterialTheme.colorScheme.primary
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(passwordResult) {
        passwordResult?.onSuccess {
            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            onDismiss()
            viewModel.clearResults()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearResults()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("Current Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPassword.isNotBlank() && newPassword.length >= 6) {
                        viewModel.changePassword(userId, currentPassword, newPassword)
                    } else if (newPassword.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 chars", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Update")
            }
        }
    )
}