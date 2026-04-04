package com.example.fitnesstracker.data.models
import com.example.fitnesstracker.R

import com.example.fitnesstracker.data.network.*
import com.example.fitnesstracker.data.models.*
import com.example.fitnesstracker.utils.*
import com.example.fitnesstracker.receivers.*
import com.example.fitnesstracker.ui.activities.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================
// POLYMORPHISM EXAMPLE: BaseWorkout and Child Classes
// ============================================
// This demonstrates OOP Polymorphism for different workout types
//
// CLASS HIERARCHY:
//
//                    BaseWorkout (Parent - Abstract)
//                         │
//      ┌──────────────────┴──────────────────┐
//      │                                     │
//  CardioWorkout                      StrengthWorkout
//      │                                     │
//   - Running                           - Weightlifting
//   - Cycling                           - Bodyweight
//   - Walking
//   - Swimming
//
// POLYMORPHISM DEMONSTRATION:
// - Same method name: calculateCalories()
// - Different implementation for each workout type
// - Running uses MET 9.8, Cycling uses MET 6.8, etc.
// ============================================

/**
 * BaseWorkout - Abstract base class for all workout types
 *
 * OOP Principles Demonstrated:
 *
 * 1. INHERITANCE
 *    - Child classes (CardioWorkout, StrengthWorkout) extend this class
 *    - They inherit all properties and methods
 *
 * 2. ENCAPSULATION
 *    - Properties are defined with access modifiers
 *    - Internal details are hidden from outside classes
 *
 * 3. POLYMORPHISM
 *    - Abstract methods must be implemented by subclasses
 *    - Each workout type calculates calories DIFFERENTLY
 *
 * 4. ABSTRACTION
 *    - Abstract class cannot be instantiated directly
 */
abstract class BaseWorkout(
    val id: Int,
    val name: String,
    val durationMinutes: Int,
    val distance: Double = 0.0,
    val notes: String = ""
) {
    // ENCAPSULATION - Properties with protected backing field
    protected var _caloriesBurned: Int = 0

    val caloriesBurned: Int
        get() = _caloriesBurned

    // ABSTRACTION - Abstract properties (must be implemented by subclasses)
    abstract val icon: ImageVector
    abstract val color: Color
    abstract val category: String

    // POLYMORPHISM - Abstract method (each workout type implements differently)
    abstract fun calculateCalories(): Int
    abstract fun getIntensity(): String

    // INHERITANCE - Open methods (can be overridden by subclasses)
    open fun getSummary(): String {
        return "$name - ${durationMinutes}min"
    }

    open fun getDetails(): String {
        val details = StringBuilder()
        details.append("Duration: $durationMinutes minutes\n")
        if (distance > 0) {
            details.append("Distance: ${String.format("%.2f", distance)} km\n")
        }
        details.append("Calories: ${calculateCalories()} kcal")
        return details.toString()
    }

    // Protected helper method for subclasses
    protected fun calculateCaloriesWithMET(met: Double, weightKg: Double = 70.0): Int {
        val durationHours = durationMinutes / 60.0
        return (met * weightKg * durationHours).toInt()
    }

    init {
        _caloriesBurned = calculateCalories()
    }
}

// ============================================
// CARDIO WORKOUT - Child Class
// ============================================
// POLYMORPHISM: calculateCalories() uses MET values
// - Running: MET 9.8 (high intensity)
// - Cycling: MET 6.8 (moderate intensity)
// - Walking: MET 3.5 (low intensity)
// - Swimming: MET 7.0 (moderate-high intensity)
// ============================================

class CardioWorkout(
    id: Int,
    name: String,
    durationMinutes: Int,
    distance: Double,
    val averageHeartRate: Int = 0,
    notes: String = ""
) : BaseWorkout(id, name, durationMinutes, distance, notes) {

    // POLYMORPHISM: Different icon for each cardio type
    override val icon: ImageVector = when (name) {
        "Running" -> Icons.Default.DirectionsRun
        "Cycling" -> Icons.Default.DirectionsBike
        "Swimming" -> Icons.Default.Pool
        "Walking" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.FitnessCenter
    }

    // POLYMORPHISM: Different color for each cardio type
    override val color: Color = when (name) {
        "Running" -> Color(0xFF4CAF50)
        "Cycling" -> Color(0xFF2196F3)
        "Swimming" -> Color(0xFF00BCD4)
        "Walking" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }

    override val category: String = "Cardio"

    // POLYMORPHISM - Same method name, DIFFERENT implementation
    // Each cardio type has different MET value for calorie calculation
    override fun calculateCalories(): Int {
        val met = when (name) {
            "Running" -> 9.8    // High intensity
            "Cycling" -> 6.8    // Moderate intensity
            "Swimming" -> 7.0   // Moderate-high intensity
            "Walking" -> 3.5    // Low intensity
            else -> 5.0
        }
        return calculateCaloriesWithMET(met)
    }

    override fun getIntensity(): String {
        return when {
            averageHeartRate > 150 -> "High"
            averageHeartRate > 120 -> "Medium"
            else -> "Low"
        }
    }

    // Override to add heart rate info
    override fun getSummary(): String {
        return "${super.getSummary()} - $averageHeartRate bpm"
    }
}

// ============================================
// STRENGTH WORKOUT - Child Class
// ============================================
// POLYMORPHISM: calculateCalories() uses different formula
// - Uses sets, reps, and weight instead of MET
// - Different calculation method than CardioWorkout
// ============================================

class StrengthWorkout(
    id: Int,
    name: String,
    durationMinutes: Int,
    val sets: Int,
    val reps: Int,
    val weight: Double = 0.0,
    notes: String = ""
) : BaseWorkout(id, name, durationMinutes, 0.0, notes) {

    override val icon: ImageVector = Icons.Default.FitnessCenter
    override val color: Color = Color(0xFFFF9800)
    override val category: String = "Strength"

    // POLYMORPHISM - Same method name, DIFFERENT implementation
    // Strength uses sets × reps formula, NOT MET values like Cardio
    override fun calculateCalories(): Int {
        // Strength training: ~5 calories per minute + effort bonus
        return (durationMinutes * 5) + (sets * reps * 2)
    }

    override fun getIntensity(): String {
        return when {
            weight > 50 -> "High"
            weight > 20 -> "Medium"
            else -> "Low"
        }
    }

    // Override to add sets/reps info
    override fun getDetails(): String {
        return "${super.getDetails()}\nSets: $sets × $reps reps"
    }
}