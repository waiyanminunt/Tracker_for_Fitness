package com.example.fitnesstracker

import retrofit2.Call
import retrofit2.http.*

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val user_id: Int?
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)

data class ActivityRequest(
    val user_id: Int,
    val activity_type: String,
    val duration: Int,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val notes: String = ""
)

data class ActivityResponse(
    val success: Boolean,
    val message: String,
    val activity_id: Int?
)

data class ActivitiesResponse(
    val success: Boolean,
    val activities: List<ActivityData>
)

data class ActivityData(
    val id: Int,
    val activity_type: String,
    val duration: Int,
    val distance: Double,
    val calories: Int,
    val notes: String,
    val created_at: String
)

interface ApiService {

    @POST("register.php")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("login.php")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("save_activity.php")
    fun saveActivity(@Body request: ActivityRequest): Call<ActivityResponse>

    @GET("get_activities.php")
    fun getActivities(@Query("user_id") userId: Int): Call<ActivitiesResponse>
}