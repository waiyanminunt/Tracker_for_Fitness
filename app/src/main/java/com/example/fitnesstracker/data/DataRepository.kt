package com.example.fitnesstracker.data

import android.util.Log
import com.example.fitnesstracker.data.network.*
import retrofit2.HttpException

class DataRepository(private val apiService: ApiService) {

    private val TAG = "DataRepository"

    private fun httpErrorMessage(e: HttpException): String {
        val code = e.code()
        return when (code) {
            404 -> "Endpoint not found (404). Check backend routes."
            400 -> "Bad request (400). Check request parameters."
            401 -> "Unauthorized (401). Please log in again."
            422 -> "Validation error (422). Check data format."
            500 -> "Server error (500). Backend may be down."
            else -> "HTTP Error $code"
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.login(request)
            if (response.success) Result.success(response)
            else Result.failure(Exception(response.message))
        } catch (e: HttpException) {
            Log.e(TAG, "login HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "login error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            if (response.success) Result.success(response)
            else Result.failure(Exception(response.message))
        } catch (e: HttpException) {
            Log.e(TAG, "register HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "register error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getActivities(userId: Int): Result<ActivitiesResponse> {
        return try {
            val response = apiService.getActivities(userId)
            if (response.success) Result.success(response)
            else Result.failure(Exception("Failed to load activities"))
        } catch (e: HttpException) {
            Log.e(TAG, "getActivities HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "getActivities error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun saveActivity(request: ActivityRequest): Result<ActivityResponse> {
        return try {
            val response = apiService.saveActivity(request)
            if (response.success) Result.success(response)
            else Result.failure(Exception(response.message))
        } catch (e: HttpException) {
            Log.e(TAG, "saveActivity HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "saveActivity error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: Int): Result<UserResponse> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.success) Result.success(response)
            else Result.failure(Exception(response.message))
        } catch (e: HttpException) {
            Log.e(TAG, "getUserProfile HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "getUserProfile error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: Int, name: String, email: String): Result<UserResponse> {
        return try {
            val request = UpdateProfileRequest(userId, name, email)
            val response = apiService.updateProfile(request)
            if (response.success) Result.success(response)
            else Result.failure(Exception(response.message))
        } catch (e: HttpException) {
            Log.e(TAG, "updateUser HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "updateUser error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest): Result<ChangePasswordResponse> {
        return try {
            val response = apiService.changePassword(request)
            if (response.success) Result.success(response)
            else Result.failure(Exception(response.message))
        } catch (e: HttpException) {
            Log.e(TAG, "changePassword HTTP error: ${e.code()} ${e.message()}")
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Log.e(TAG, "changePassword error: ${e.message}")
            Result.failure(e)
        }
    }
}
