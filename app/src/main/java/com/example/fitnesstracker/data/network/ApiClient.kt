package com.example.fitnesstracker.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Change this to your computer's IP address
    // For emulator use: http://10.0.2.2/fitnesstracker_api/
    // For real device use: http://COMPUTER_IP/fitnesstracker_api/


    private const val BASE_URL = "http://192.168.100.97/fitnesstracker_api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}