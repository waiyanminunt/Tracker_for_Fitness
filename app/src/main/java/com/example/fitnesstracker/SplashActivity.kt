package com.example.fitnesstracker

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SplashActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Play splash sound
        mediaPlayer = MediaPlayer.create(this, R.raw.splash_sound)
        mediaPlayer?.start()

        setContent {
            SplashScreen()
        }

        // Navigate to LoginActivity after 2.5 seconds
        android.os.Handler(mainLooper).postDelayed({
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2500)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
fun SplashScreen() {
    val darkPurple = Color(0xFF1A0A2E)
    val lightPurple = Color(0xFF6B4C9A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkPurple),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glitch Logo
        GlitchLogo(
            imageRes = R.drawable.pulse,
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Loading text
        Text(
            text = "Loading...",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Loading spinner
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = lightPurple,
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun GlitchLogo(
    imageRes: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")

    // Strong shake animation
    val shake by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    // Strong flicker animation
    val flicker by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(40, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    // Strong color shift animation
    val colorShift by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorShift"
    )

    // Scale glitch
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.clipToBounds()
    ) {
        // Red shadow (shifted left)
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Logo Red",
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = colorShift + shake
                    alpha = 0.7f
                    scaleX = scale
                    scaleY = scale
                }
        )

        // Blue shadow (shifted right)
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Logo Blue",
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = -colorShift - shake
                    alpha = 0.7f
                    scaleX = scale
                    scaleY = scale
                }
        )

        // Main logo
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "App Logo",
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = shake / 2
                    alpha = flicker
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}