package com.example.fitnesstracker.ui.activities

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.R
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitnesstrackerTheme {
                SplashScreen()
            }
        }
    }

    @Composable
    fun SplashScreen() {
        val context = LocalContext.current
        val backgroundColor = MaterialTheme.colorScheme.background
        val primaryColor = MaterialTheme.colorScheme.primary

        // Animation for logo scaling
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteTransitionSpec(),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Animated Logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.heart_pulse),
                        contentDescription = "Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "FITNESS TRACKER",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Track. Improve. Repeat.",
                    color = primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Navigate after 3 seconds
        LaunchedEffect(Unit) {
            // Play splash sound (optional, assuming sound exists)
            try {
                val mediaPlayer = MediaPlayer.create(context, R.raw.splash_sound)
                mediaPlayer.start()
            } catch (e: Exception) {
                // Sound file might not exist, ignore
            }

            kotlinx.coroutines.delay(3000)
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
            finish()
        }
    }

    private fun infiniteTransitionSpec() = infiniteRepeatable<Float>(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}