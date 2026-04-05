package com.example.fitnesstracker

import android.app.Application
import com.example.fitnesstracker.utils.ThemeManager

/**
 * Custom Application class — runs once when the app process starts,
 * before any Activity is created.
 *
 * Loads the saved "isDarkMode" preference into ThemeManager so that
 * every Activity's FitnesstrackerTheme{} picks up the correct value
 * from the very first frame, with zero flicker.
 *
 * IMPORTANT: Register this in AndroidManifest.xml:
 *   android:name=".FitnessApp"
 */
class FitnessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("FitnessTrackerPrefs", MODE_PRIVATE)
        ThemeManager.isDarkMode.value = prefs.getBoolean("isDarkMode", false)
    }
}
