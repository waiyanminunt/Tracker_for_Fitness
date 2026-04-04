package com.example.fitnesstracker.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnesstrackerTheme {
                PrivacyPolicyScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

val fitnessPrivacyPolicyText = """
    Privacy Policy for Fitness Tracker
    
    1. GPS Location Tracking
    Our app collects background and foreground location data to accurately track your runs, walks, and cycling sessions. This data is only collected when you actively start a workout and have granted explicit location permissions.
    
    2. Local Activity Data Storage
    All personal fitness data, including step counts, calories burned, and activity history, are securely stored locally on your device or synchronized to your personal cloud account. We do not sell or share your raw health data with third-party advertisers.
    
    3. Analytics & Crash Reports
    To improve user experience, we may collect anonymous, aggregated usage data and crash reports. This helps us identify bugs and optimize app performance without compromising your individual privacy. You can opt out of analytics through the Privacy Settings menu.
    
    4. Data Protection
    We implement industry-standard security measures, including data encryption in transit and at rest, to keep your information safe from unauthorized access.
    
    By continuing to use this application, you agree to these terms.
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Privacy Policy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor
            )
        )

        // Scrollable Text Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = fitnessPrivacyPolicyText,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = onBackgroundColor.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
