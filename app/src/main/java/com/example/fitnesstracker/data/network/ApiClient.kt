package com.example.fitnesstracker.data.network

import android.util.Log
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * ApiClient — Singleton Retrofit + OkHttp configuration.
 *
 * Performance:
 *   1. 10 MB OkHttp disk cache + max-stale=30s interceptor for GET responses.
 *      Call [initCache] once from Application.onCreate() before any API call.
 *   2. OkHttp adds Accept-Encoding: gzip automatically — no extra interceptor needed.
 *   3. URL logging always active; body logging always active (safe for debug builds).
 *   4. Timeouts reduced to 10 s connect / 15 s read (LAN server — no need for 30 s).
 */
object ApiClient {
    private const val TAG                = "ApiClient"
    private const val EMULATOR_IP        = "10.0.2.2"
    private const val PHYSICAL_DEVICE_IP = "192.168.100.97"

    // ── Interceptors ──────────────────────────────────────────────────────────

    /** Logs every request URL + HTTP status code to Logcat. */
    private val urlLoggingInterceptor = Interceptor { chain ->
        val request  = chain.request()
        Log.d(TAG, "→ API CALL: [${request.method}] ${request.url}")
        val response: Response = chain.proceed(request)
        Log.d(TAG, "← API RESPONSE: ${response.code} for ${request.url}")
        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP ERROR ${response.code}: ${request.url}")
        }
        response
    }

    /**
     * Forces OkHttp to serve cached GET responses that are up to 30 seconds old,
     * even when the PHP backend sends no Cache-Control headers.
     * POST requests (login, save-activity) are never cached.
     */
    private val cacheInterceptor = Interceptor { chain ->
        val request = chain.request()
        val newRequest = if (request.method == "GET") {
            request.newBuilder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxStale(30, TimeUnit.SECONDS)
                        .build()
                )
                .build()
        } else {
            request
        }
        chain.proceed(newRequest)
    }

    /** Logs full request/response bodies to Logcat. */
    private val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ── Base URL ──────────────────────────────────────────────────────────────

    private val baseUrl: String by lazy {
        val fingerprint  = android.os.Build.FINGERPRINT.lowercase()
        val model        = android.os.Build.MODEL.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()

        val isEmulator = fingerprint.contains("generic") ||
                fingerprint.contains("vbox") ||
                fingerprint.contains("sdk_gphone") ||
                model.contains("emulator") ||
                model.contains("google_sdk") ||
                model.contains("android sdk built for x86") ||
                manufacturer.contains("genymotion") ||
                (manufacturer.contains("unknown") && model.contains("sdk"))

        // Emulator  → routes to host machine via Android's virtual router (10.0.2.2)
        // Physical  → connects to Laragon on the laptop's LAN IP
        val url = if (isEmulator)
            "http://$EMULATOR_IP:8000/phps/"
        else
            "http://$PHYSICAL_DEVICE_IP/fitnesstracker_api/"

        Log.d(TAG, "Using base URL: $url  (isEmulator=$isEmulator)")
        url
    }

    // ── HTTP Cache ────────────────────────────────────────────────────────────

    /**
     * 10 MB on-disk response cache stored in the app's cache directory.
     * Must be initialized by calling [initCache] in Application.onCreate()
     * before the first Retrofit request is made.
     * Safe to skip — OkHttpClient accepts null cache and falls back to no-cache.
     */
    private var httpCache: Cache? = null

    /**
     * Initializes the OkHttp disk cache.
     * Call once from your Application class before any network request.
     *
     * @param cacheDir  The app's cache directory (e.g. context.cacheDir).
     */
    fun initCache(cacheDir: File) {
        if (httpCache == null) {
            httpCache = Cache(
                directory = File(cacheDir, "okhttp_cache"),
                maxSize   = 10L * 1024L * 1024L   // 10 MB
            )
            Log.d(TAG, "OkHttp cache initialized → ${cacheDir.absolutePath}/okhttp_cache")
        }
    }

    // ── OkHttpClient ──────────────────────────────────────────────────────────

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(httpCache)                       // null-safe: no-op if initCache not called
            .addInterceptor(cacheInterceptor)       // max-stale=30 s for GET requests
            .addInterceptor(urlLoggingInterceptor)  // URL + status code log
            .addInterceptor(httpLoggingInterceptor) // full body log
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // ── Retrofit ──────────────────────────────────────────────────────────────

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}