/**
 * FitnessTracker Pro — Network Layer
 *
 * This file contains all Retrofit API definitions and their associated
 * request/response data models for the FitnessTracker backend (FastAPI).
 *
 * Architecture Note (SOLID — Interface Segregation Principle):
 *   The [ApiService] interface acts as the single contract between the
 *   network layer and [DataRepository]. All models are defined here
 *   as immutable data classes to prevent unintended mutation.
 *
 * Naming Conventions:
 *   - Request DTOs  → [Feature]Request  (e.g. [LoginRequest])
 *   - Response DTOs → [Feature]Response (e.g. [LoginResponse])
 *   - Domain Models → Plain noun         (e.g. [User], [ActivityData])
 */
package com.example.fitnesstracker.data.network

import retrofit2.http.*

// ─── Authentication Models ────────────────────────────────────────────────────

/** Credentials sent by the user when signing in. */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Server response after a login attempt.
 * @property success True if authentication succeeded.
 * @property message Human-readable result or error description.
 * @property user Non-null on success; contains the authenticated user's profile.
 */
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)

/** Profile details submitted when creating a new account. */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

/**
 * Server response after account creation.
 * @property user_id The ID assigned to the new account, or null on failure.
 */
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val user_id: Int?
)

// ─── User / Profile Models ────────────────────────────────────────────────────

/**
 * Lightweight domain model representing a registered user.
 * Used both in [LoginResponse] and [UserResponse].
 */
data class User(
    val id: Int,
    val name: String,
    val email: String
)

/** Fields submitted when the user edits their profile. */
data class UpdateProfileRequest(
    val user_id: Int,
    val name: String,
    val email: String
)

/**
 * Generic server response that carries a [User] object.
 * Used for both fetching and updating the user profile.
 */
data class UserResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)

/** Payload for a password change request. */
data class ChangePasswordRequest(
    val user_id: Int,
    val current_password: String,
    val new_password: String
)

/** Server acknowledgment of a password change attempt. */
data class ChangePasswordResponse(
    val success: Boolean,
    val message: String
)

// ─── Activity / Fitness Models ────────────────────────────────────────────────

/**
 * Payload sent when saving a completed fitness session.
 *
 * @property user_id     Owner of the activity.
 * @property activity_type One of: "Running", "Walking", "Cycling", "Hiking".
 * @property duration    Total active time in **minutes**.
 * @property distance    Total distance covered in **kilometres**.
 * @property calories    Estimated energy expenditure in kcal.
 * @property notes       Optional free-text annotation from the user.
 */
data class ActivityRequest(
    val user_id: Int,
    val activity_type: String,
    val duration: Int,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val notes: String = ""
)

/**
 * Server acknowledgment of a saved activity.
 * @property activity_id The database ID of the newly created record, or null on failure.
 */
data class ActivityResponse(
    val success: Boolean,
    val message: String,
    val activity_id: Int?
)

/**
 * Paginated list of activities returned by [ApiService.getActivities].
 * The outer wrapper exists for API consistency; the real payload is [activities].
 */
data class ActivitiesResponse(
    val success: Boolean,
    val activities: List<ActivityData>
)

/**
 * A single historical fitness record retrieved from the backend.
 *
 * @property id          Unique database identifier.
 * @property activity_type One of: "Running", "Walking", "Cycling", "Hiking".
 * @property duration    Active time in **minutes**.
 * @property distance    Distance covered in **kilometres**.
 * @property calories    Estimated energy expenditure in kcal.
 * @property notes       Optional free-text annotation.
 * @property created_at  ISO-8601 timestamp of when the record was created.
 */
data class ActivityData(
    val id: Int,
    val activity_type: String,
    val duration: Int,
    val distance: Double,
    val calories: Int,
    val notes: String,
    val created_at: String
)

// ─── API Contract ─────────────────────────────────────────────────────────────

/**
 * Retrofit interface defining the full HTTP contract with the FitnessTracker
 * FastAPI backend.
 *
 * Base URL: configured in [ApiClient] → `http://192.168.100.97/fitnesstracker_api/` (physical)
 *                                       `http://10.0.2.2:8000/phps/` (emulator)
 *
 * All endpoints are `suspend` functions to support Kotlin coroutines.
 * Route names use **snake_case** to match FastAPI's default route convention.
 *
 * Scalability Note:
 *   To add a new feature (e.g. "Supporter Unit"), define its request/response
 *   models above and add endpoint declarations here. [DataRepository] then
 *   wraps those calls in [Result] wrappers — no other layer needs to change.
 */
interface ApiService {

    // ── Authentication ────────────────────────────────────────────────────────

    /** Creates a new user account. Returns the assigned user ID on success. */
    @POST("register.php")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    /** Validates credentials and returns the authenticated [User] on success. */
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // ── Activity CRUD ─────────────────────────────────────────────────────────

    /**
     * Persists a completed fitness session.
     * Called by [DataRepository.saveActivity] after the user taps "FINISH".
     */
    @POST("save_activity.php")
    suspend fun saveActivity(@Body request: ActivityRequest): ActivityResponse

    /**
     * Fetches all historical activities for the given user.
     * Used by both the Dashboard and Statistics screens.
     *
     * @param userId The authenticated user's ID (from SharedPreferences).
     */
    @GET("get_activities.php")
    suspend fun getActivities(@Query("user_id") userId: Int): ActivitiesResponse

    // ── Profile Management ────────────────────────────────────────────────────

    /** Fetches the full profile for the given user ID. */
    @GET("get_profile.php")
    suspend fun getUserProfile(@Query("user_id") userId: Int): UserResponse

    /** Updates the user's display name and email address. */
    @POST("update_profile.php")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserResponse

    /** Changes the user's password after validating the current one. */
    @POST("change_password.php")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordResponse
}