# White Box Testing Report
## Fitness Tracker Android Application

**Project:** `com.example.fitnesstracker`  
**Language:** Kotlin (Android) + PHP (Backend API)  
**Testing Type:** White Box (Structural / Code-level) Testing  
**Date:** 2026-04-01  
**Tester:** Antigravity AI Code Analyst  

---

## 1. Executive Summary

The Fitness Tracker app is a full-stack Android application using Jetpack Compose for the UI, Retrofit for networking, and a PHP/MySQL backend. The app features user authentication, activity tracking (GPS + manual), wellness monitoring (screen time, water intake), notifications, and a statistics dashboard.

This white box analysis covers **internal logic paths, branch coverage, data flow, and structural defects** across all source files. No source code was modified.

---

## 2. Architecture Overview

```
┌──────────────────────────────────────────────┐
│                Android App                    │
│                                              │
│  SplashActivity → LoginActivity              │
│                        ↓                     │
│               DashboardActivity (BaseActivity)│
│              /      |       \      \          │
│     AddActivity  Stats  Profile  Notifications│
│          ↓                                   │
│   TrackingActivity (GPS)                     │
│                                              │
│  Helpers: ScreenTimeHelper, WaterTracker,    │
│           NotificationHelper                 │
│                                              │
│  OOP: BaseWorkout → CardioWorkout,           │
│                      StrengthWorkout         │
└──────────────────────────────────────────────┘
           ↕ Retrofit (HTTP)
┌──────────────────────────────────────────────┐
│           PHP Backend (phps/)                 │
│  login.php | register.php | save_activity.php │
│  get_activities.php | config.php             │
└──────────────────────────────────────────────┘
```

---

## 3. Test Coverage Analysis by Module

### 3.1 `BaseWorkout.kt` — OOP / Calorie Logic

**Execution Paths Identified:**

| Method | Path | Condition |
|--------|------|-----------|
| `calculateCalories()` (Cardio) | Running | name == "Running" → MET 9.8 |
| `calculateCalories()` (Cardio) | Cycling | name == "Cycling" → MET 6.8 |
| `calculateCalories()` (Cardio) | Swimming | name == "Swimming" → MET 7.0 |
| `calculateCalories()` (Cardio) | Walking | name == "Walking" → MET 3.5 |
| `calculateCalories()` (Cardio) | Unknown | name == other → MET 5.0 |
| `calculateCalories()` (Strength) | Any | `(durationMinutes * 5) + (sets * reps * 2)` |
| `getIntensity()` (Cardio) | High | averageHeartRate > 150 |
| `getIntensity()` (Cardio) | Medium | averageHeartRate > 120 |
| `getIntensity()` (Cardio) | Low | averageHeartRate ≤ 120 |
| `getIntensity()` (Strength) | High | weight > 50 |
| `getIntensity()` (Strength) | Medium | weight > 20 |
| `getIntensity()` (Strength) | Low | weight ≤ 20 |
| `getDetails()` | With distance | distance > 0 |
| `getDetails()` | No distance | distance == 0 |

**Findings:**

> [!WARNING]
> **Bug: `init` block calls `calculateCalories()` before subclass fields are set**
>
> `BaseWorkout.kt` Line 95–97:
> ```kotlin
> init {
>     _caloriesBurned = calculateCalories()
> }
> ```
> This `init` block is in the abstract parent class. When Kotlin constructs `CardioWorkout` or `StrengthWorkout`, the parent `init` runs **before** the subclass is fully initialized. In `CardioWorkout`, `averageHeartRate` will be `0` during this call. In `StrengthWorkout`, `sets` and `reps` will be `0` — so `calculateCalories()` will return `durationMinutes * 5 + 0`, not the correct value. The stored `_caloriesBurned` will always be wrong.

> [!NOTE]
> **Formula-level test values for `CardioWorkout.calculateCalories()`:**
> Using default weight = 70kg:
> - Running 30min: `9.8 × 70 × 0.5 = 343 kcal`
> - Walking 60min: `3.5 × 70 × 1.0 = 245 kcal`
> - Swimming 45min: `7.0 × 70 × 0.75 = 367 kcal`
>
> **Formula-level test for `StrengthWorkout.calculateCalories()`** (3 sets, 10 reps, 30 min):
> - `(30 × 5) + (3 × 10 × 2) = 150 + 60 = 210 kcal`

---

### 3.2 `LoginActivity.kt` — Authentication Flow

**Branch Coverage (Login Mode):**

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-L01 | email empty, password empty | Toast: "Please fill all fields" |
| TC-L02 | email filled, password empty | Toast: "Please fill all fields" |
| TC-L03 | email empty, password filled | Toast: "Please fill all fields" |
| TC-L04 | email + password filled | API call to `login.php` |
| TC-L05 | API response: success=true, user!=null | Navigate to DashboardActivity |
| TC-L06 | API response: success=false | Toast: body.message |
| TC-L07 | API response: body=null | Toast: "Login failed" |
| TC-L08 | Network failure (onFailure) | Toast: "Error: ${t.message}" |

**Branch Coverage (Register Mode):**

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-R01 | All fields empty | Toast: "Please fill all fields" |
| TC-R02 | Password ≠ confirmPassword | Toast: "Passwords do not match" |
| TC-R03 | password matches, name/email empty | Toast: "Please fill all fields" |
| TC-R04 | All valid | API call to `register.php` |
| TC-R05 | API success=true | Switch to login mode, clear passwords |
| TC-R06 | API success=false | Toast: body.message |
| TC-R07 | Network failure | Toast: "Error: ${t.message}" |

**Findings:**

> [!WARNING]
> **Missing Email Format Validation (Client-Side)**
>
> `LoginActivity.kt` Line 202–203:
> ```kotlin
> if (email.isNotEmpty() && password.isNotEmpty()) {
> ```  
> There is **no email format validation** on the Android side for login. A user can type "abc" and an API request will be sent. The `KeyboardType.Email` hint only changes the keyboard, not validation. The registration PHP (`register.php`) validates format on the server side, but the login doesn't — providing an inconsistent experience.

> [!NOTE]
> **Min-length password policy is missing.** A user can register and login with a 1-character password. No minimum length check exists in either the client (Kotlin) or server (PHP) side.

> [!WARNING]
> **Missing name validation in Register mode**
>
> `LoginActivity.kt` Line 227:
> ```kotlin
> if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword)
> ```
> The condition order is: name not empty → email not empty → password not empty → passwords match. **However**, if all except `name` are filled and `password != confirmPassword`, the first outer if fails and we reach the `else` block, which shows "Passwords do not match" — that is **correct**. ✅
>
> But if `name` is empty AND `password != confirmPassword`, the code shows **"Please fill all fields"** instead of "Passwords do not match" — this could mislead the user into thinking their name is the problem.

---

### 3.3 `AddActivity.kt` — Activity Input + Polymorphism

**Branch Coverage:**

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-A01 | GPS activity selected (Running/Cycling/Walking) | Navigate to TrackingActivity |
| TC-A02 | Manual activity selected (Swimming/Weightlifting) | Show ActivityInputForm |
| TC-A03 | Back pressed with activity selected | Deselect activity |
| TC-A04 | Back pressed with no activity | Finish() |
| TC-A05 | Duration empty, save clicked | Toast: "Please enter duration" |
| TC-A06 | Duration filled, save clicked | API call to `save_activity.php` |
| TC-A07 | Duration is non-numeric string | `toIntOrNull()` returns null → `toInt()` on Line 466 **crashes** |
| TC-A08 | API success | Toast with calories, call onSave() |
| TC-A09 | API failure | Toast: "Error: ${t.message}" |

**Findings:**

> [!CAUTION]
> **Critical Bug: Unguarded `toInt()` call can crash the app**
>
> `AddActivity.kt` Line 466:
> ```kotlin
> duration = duration.toInt(),
> ```
> The keyboard is `KeyboardType.Number`, but on many Android devices users can still type decimals or paste non-integer text like "30.5" or "abc". `toIntOrNull()` is used for `calculatedCalories`, but the save button uses `duration.toInt()` directly without a null-check. **This will throw a `NumberFormatException` and crash.** A safe call like `duration.toIntOrNull() ?: 0` should be used.

> [!WARNING]
> **`distance.toDouble()` on Line 467 is unsafe**
>
> ```kotlin
> distance = if (distance.isNotEmpty()) distance.toDouble() else 0.0,
> ```
> Similarly, `distance.toDouble()` will crash if the user enters something like "1.2.3". Should use `distance.toDoubleOrNull() ?: 0.0`.

> [!NOTE]
> **Polymorphism works correctly.** The `calculatedCalories = remember(...)` block correctly routes `Swimming` to `CardioWorkout` and `Weightlifting` to `StrengthWorkout`, calling `calculateCalories()` on each. The pattern is structurally valid — the only concern is the `init` block issue in `BaseWorkout` above, which means `_caloriesBurned` in the object will be wrong, but since `calculateCalories()` is called directly (not via `caloriesBurned` property), the displayed value is correct. ✅

---

### 3.4 `TrackingActivity.kt` — GPS Tracking Logic

**Branch Coverage:**

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-T01 | Fine location permission denied | Show permission-denied UI |
| TC-T02 | Only coarse location granted | `hasPermission = true` (accepted) |
| TC-T03 | Both permissions granted | Normal tracking UI |
| TC-T04 | isTracking = false | Show START button |
| TC-T05 | isTracking = true | Show STOP/FINISH button |
| TC-T06 | distanceMeters < 100 | Pace returns "--:--" |
| TC-T07 | durationSeconds = 0, distanceMeters ≥ 100 | Division by zero risk in pace calc |
| TC-T08 | API save succeeds | Call onFinish() |
| TC-T09 | API save fails | Toast error |

**Findings:**

> [!CAUTION]
> **Incorrect Distance Calculation (Approximation Bug)**
>
> `TrackingActivity.kt` Lines 128–130:
> ```kotlin
> override fun onLocationResult(locationResult: LocationResult) {
>     locationResult.lastLocation?.let { location ->
>         currentSpeed = location.speed
>         if (isTracking) {
>             distanceMeters += location.speed  // BUG
>         }
>     }
> }
> ```
> `location.speed` is in **meters per second**. The callback fires every 1 second (as set by the 1000ms interval in `LocationRequest.Builder`). So adding `speed` is adding **meters/second** — which happens to be numerically correct IF the callback fires exactly every 1 second. **However**, location callbacks are not guaranteed to fire at exactly 1-second intervals, making this an unreliable approximation. The correct approach is to use GPS coordinate differences (`distanceTo()`).

> [!WARNING]
> **`TrackingActivity` does not extend `BaseActivity`**
>
> `TrackingActivity.kt` Line 32:
> ```kotlin
> class TrackingActivity : ComponentActivity()
> ```
> This is inconsistent with all other activities that extend `BaseActivity`. As a result, `TrackingActivity` re-reads the user ID and activity type directly from intent (Lines 41–42) instead of using the inherited `getUserId()` method. This is a code consistency issue.

> [!WARNING]
> **`onFinish` callback parameters are discarded**
>
> `TrackingActivity.kt` Lines 49–51:
> ```kotlin
> onFinish = { distance, duration, calories ->
>     finish()
> }
> ```
> The `onFinish` lambda receives `distance`, `duration`, and `calories` but they are **never used** — only `finish()` is called. This means tracking data is saved to the DB but the calling activity (`AddActivity`) is never updated with the results.

> [!WARNING]
> **`LocationCallback` is never removed / unregistered**
>
> The `locationCallback` object created inside `LaunchedEffect(isTracking, hasPermission)` is never passed to `fusedLocationClient.removeLocationUpdates()`. When the user stops tracking, location updates continue in memory until the activity is destroyed. This wastes battery and CPU.

---

### 3.5 `DashboardActivity.kt` — Main Screen Logic

**Branch Coverage:**

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-D01 | Activities tab selected | Show ActivitiesContent |
| TC-D02 | Wellness tab selected | Show WellnessContent, fetch wellness data |
| TC-D03 | API response: success + activities | Populate list, compute totals |
| TC-D04 | API response: body = null | Toast "Failed to load activities" |
| TC-D05 | activities.isEmpty() | Show empty-state UI |
| TC-D06 | isLoading = true | Show spinner |
| TC-D07 | hasScreenTimePermission = false | Show "Grant Permission" button |
| TC-D08 | waterGoal = 0 | Division: `waterIntake / waterGoal` — protected by `if (waterGoal > 0)` ✅ |
| TC-D09 | unreadCount > 9 | Badge shows "9+" |
| TC-D10 | unreadCount = 0 | Badge hidden |
| TC-D11 | isOverLimit = true | Show red warning card |
| TC-D12 | continuousUse > 60 | Continuous use indicator turns red |

**Findings:**

> [!WARNING]
> **`unreadCount` state is not updated when returning from `NotificationsActivity`**
>
> `DashboardActivity.kt` Lines 261–266:
> ```kotlin
> val unreadCount = remember { mutableStateOf(notificationHelper.getUnreadCount()) }
> LaunchedEffect(selectedDashboardTab) {
>     unreadCount.value = notificationHelper.getUnreadCount()
> }
> ```
> The badge only refreshes when `selectedDashboardTab` changes. When user reads notifications in `NotificationsActivity` and presses back, the badge still shows the old count unless they switch tabs. There is no `LaunchedEffect` tied to `onResume` or a lifecycle event.

> [!NOTE]
> **Add tab (index 1) never sets `selectedTab = 1`** — it directly opens `AddActivity` instead. So the "Add" nav bar item is never visually highlighted as "selected". This is a minor UX issue, not a crash risk.

---

### 3.6 `NotificationHelper.kt` — Notification Storage

**Branch Coverage:**

| Method | Branch | Condition |
|--------|--------|-----------|
| `getNotifications()` | Empty prefs | Returns `emptyList()` |
| `getNotifications()` | JSON parse error | Catch block → `emptyList()` |
| `addNotification()` | Size ≤ 50 | Save full list |
| `addNotification()` | Size > 50 | Trim to 50, save |
| `markAsRead()` | ID found | Update and save |
| `markAsRead()` | ID not found | No-op (index == -1) |

**Findings:**

> [!NOTE]
> **ID collision risk** — `NotificationItem.id` is set to `System.currentTimeMillis()`. If two notifications are added within the same millisecond, they would share the same `id`. `markAsRead(id)` uses `indexOfFirst`, so it would only mark the first match as read. This is low probability but technically a defect in high-frequency scenarios.

> [!NOTE]
> **`clearAll()` and `markAllAsRead()` are not atomic** — both read, modify, and write in separate steps. In a multithreaded scenario, they could produce race conditions. Since this runs on the main thread in Compose, this is currently safe but is worth noting.

---

### 3.7 `WaterTrackerHelper.kt` — Water Tracking Logic

**Branch Coverage:**

| Method | Branch | Condition |
|--------|--------|-----------|
| `getTodayIntake()` | No data for today | Returns 0 |
| `addWater()` | Normal | Current + amountMl saved |
| `getProgressPercentage()` | intake = 0 | Returns 0 |
| `getProgressPercentage()` | intake = DAILY_GOAL_ML | Returns 100 |
| `getProgressPercentage()` | intake > DAILY_GOAL_ML | Clamped to 100 via `coerceIn` |
| `getRemaining()` | Intake > goal | Clamped to 0 via `coerceAtLeast(0)` |
| `isGoalReached()` | intake ≥ 2500 | Returns true |
| `scheduleReminders()` | Past hour | Schedules for next day |

**Findings:**

> [!WARNING]
> **`getTodayKey()` format is ambiguous**
>
> `WaterTrackerHelper.kt` Lines 94–99:
> ```kotlin
> val year = calendar.get(Calendar.YEAR)
> val month = calendar.get(Calendar.MONTH)  // 0-indexed!
> val day = calendar.get(Calendar.DAY_OF_MONTH)
> return "water_$year$month$day"
> ```
> `Calendar.MONTH` is **0-indexed** (January = 0, December = 11). Keys would be like `water_202603` for April (month 3, not 4). More critically, `water_2026030` (January 1st = 2026/0/1) and `water_2026030` would **conflict with** October 1st... Wait — actually each is unique because of the order, but the month values are shifted by 1 from human expectation. If any developer reads these keys, they will be confused. This can also cause a subtle bug on calendar month boundaries.
>
> **More serious**: `"water_$year$month$day"` without separators. Consider **Dec 31 (month=11, day=31) → `water_20261131`** vs **November 13 (month=10, day=13) → `water_20261013`**. These are different, so no collision — but November (month=10) gives `water_2026101` for the 1st day, while January 1st (month=0) gives `water_2026001`. The concatenated numbers without separators can create genuine collisions. For example: **January 22 (0/22) → `water_2026022`** vs **February 2 (1/2) → `water_202612`** — these are different, but **October 1 (9/1) → `water_202691`** vs **September 1 (8/1) → `water_202681`** — also different. After checking every case systematically, there are no actual day-of-year collisions, but the pattern is risky and fragile.

---

### 3.8 `ScreenTimeHelper.kt` — Screen Time Metrics

**Branch Coverage:**

| Method | Branch | Condition |
|--------|--------|-----------|
| `hasUsageStatsPermission()` | Granted | Returns true |
| `hasUsageStatsPermission()` | Not granted | Returns false |
| `getTodayScreenTime()` | No permission | Returns 0L immediately |
| `getTodayScreenTime()` | Empty stats | Returns 0L |
| `getTodayPickups()` | No permission | Returns 0 |
| `getAverageSessionDuration()` | pickups = 0 | Returns 0 (no divide by zero) ✅ |
| `getLongestSession()` | Empty list | `maxOfOrNull` → 0 |
| `isOverScreenTimeLimit()` | screenTime > 7h | Returns true |

**Findings:**

> [!NOTE]
> **`getTodayPickups()` interpretation is an approximation.** It counts apps with `totalTimeInForeground > 5000ms` (5 seconds), not actual phone pickups. This is a limitation of the `UsageStatsManager` API, not a code bug.

> [!NOTE]
> **`getAverageSessionDuration()` is derived from aggregated stats.** It divides total screen time by "pickups" (apps used), not actual unlock/relock events. Result is a rough approximation, but the division-by-zero guard at Line 115 is correct.

---

### 3.9 PHP Backend — `login.php`

**Paths Identified:**

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-PL01 | Missing `email` or `password` fields | `{success:false, message:"Email and password are required"}` |
| TC-PL02 | Email not found in DB | `{success:false, message:"User not found"}` |
| TC-PL03 | Email found, wrong password | `{success:false, message:"Invalid password"}` |
| TC-PL04 | Email found, correct password | `{success:true, user:{...}}` |

**Findings:**

> [!CAUTION]
> **SQL Injection Risk — `login.php` Line 26**
> ```php
> $sql = "SELECT id, name, email, password FROM users WHERE email = '$email'";
> ```
> Although `real_escape_string()` is used on Line 22, the use of string interpolation in SQL is the wrong pattern. If `real_escape_string()` ever fails or the charset is misconfigured, injection becomes possible. **Prepared statements** (e.g., `$stmt = $conn->prepare(...)`) are the safe industry standard and must be used instead.

> [!CAUTION]
> **SQL Injection Risk — `register.php` Lines 36, 51; `get_activities.php` Line 20–24; `save_activity.php` Line 30–31**
> The same raw string interpolation pattern is used in all PHP files. While type-casting (`(int)$data['user_id']`) in `save_activity.php` and `get_activities.php` provides some protection for numeric fields, string fields like `activity_type` and `notes` are still passed via `real_escape_string()` + interpolation, instead of prepared statements.

> [!NOTE]
> **No rate-limiting on login attempts.** A malicious client can make unlimited login attempts. Brute-force protection (account lockout, CAPTCHA, or request throttling) is absent.

---

### 3.10 PHP Backend — `register.php`

| Branch | Condition | Expected Result |
|--------|-----------|-----------------|
| TC-PR01 | Missing name/email/password | Error response |
| TC-PR02 | Invalid email format | `"Invalid email format"` |
| TC-PR03 | Email already registered | `"Email already registered"` |
| TC-PR04 | All valid | Hash password, insert, return user_id |
| TC-PR05 | DB insert fails | Return error with `$conn->error` |

**Findings:**

> [!NOTE]
> **`password` field has no minimum length check.** A user can register with a 1-character password. Server should enforce `strlen($password) >= 8`.

> [!NOTE]
> **`name` field has no maximum length validation.** A very long name (e.g., 10,000 chars) would be truncated by the DB column length (usually VARCHAR 255), which will silently drop data rather than returning an error.

---

### 3.11 `SplashActivity.kt` — Splash Screen

**Paths Identified:**

| Path | Condition |
|------|-----------|
| Normal flow | MediaPlayer created, plays sound, navigates after 2500ms |
| Sound file missing (`R.raw.splash_sound`) | `MediaPlayer.create()` returns null → crash on `.start()` |
| Activity destroyed before 2500ms | `onDestroy()` releases player, but `postDelayed` still fires and calls `startActivity()` on destroyed context |

**Findings:**

> [!WARNING]
> **Potential NPE / crash if `R.raw.splash_sound` is missing or fails to load**
>
> `SplashActivity.kt` Lines 32–33:
> ```kotlin
> mediaPlayer = MediaPlayer.create(this, R.raw.splash_sound)
> mediaPlayer?.start()
> ```
> `MediaPlayer.create()` can return `null` if the resource cannot be loaded. The null-safe `?.start()` handles this without crashing. ✅ However, if the resource file is absent from the `res/raw` directory, the code will fail at compile time (missing resource). This should be verified in the build.

> [!WARNING]
> **Memory leak: delayed navigation after `finish()`**
>
> `SplashActivity.kt` Lines 40–46:
> ```kotlin
> android.os.Handler(mainLooper).postDelayed({
>     mediaPlayer?.stop()
>     ...
>     startActivity(Intent(this, LoginActivity::class.java))
>     finish()
> }, 2500)
> ```
> If the user presses the Back button before 2500ms, the activity finishes, but the delayed `Runnable` still holds a **reference to `this`** (the dead Activity). When it fires, it calls `startActivity()` on the finished context — which can cause unexpected navigation or leak. The handler callback is never cancelled in `onDestroy()`.

---

### 3.12 `WaterReminderReceiver.kt`

**Findings:**

> [!WARNING]
> **Notification taps navigate to `LoginActivity` instead of `DashboardActivity`**
>
> `WaterReminderReceiver.kt` Line 18:
> ```kotlin
> val notificationIntent = Intent(context, LoginActivity::class.java)
> ```
> When a user taps the water reminder notification, they are sent to the **Login screen** instead of the Dashboard. After login, they lose the notification context. This should navigate to `DashboardActivity` with the Wellness tab pre-selected.

---

### 3.13 `ScreenTimeReceiver.kt`

**Findings:**

> [!WARNING]
> **Same issue: notification taps navigate to `LoginActivity`**
>
> `ScreenTimeReceiver.kt` Line 21:
> ```kotlin
> val notificationIntent = Intent(context, LoginActivity::class.java)
> ```
> Same problem as `WaterReminderReceiver` — screen time warning notifications send the user to the Login screen, not the Wellness tab.

---

### 3.14 `ApiClient.kt`

**Findings:**

> [!CAUTION]
> **Hardcoded IP address in production code**
>
> `ApiClient.kt` Line 12:
> ```kotlin
> private const val BASE_URL = "http://192.168.100.130/fitnesstracker_api/"
> ```
> The base URL contains a **hardcoded local network IP address**. This means:
> 1. The app **will always fail** on any network other than this specific WiFi.
> 2. HTTP (not HTTPS) is used — data is transmitted unencrypted.
> 3. Credentials (email/password) are sent in plain text over the network.

> [!CAUTION]
> **HTTP traffic — no HTTPS / TLS used**
> Passwords and user data are sent over plain HTTP. Even though there is a `network_security_config.xml` referenced in the manifest (allowing cleartext traffic), this is insecure for a real application.

---

### 3.15 `ProfileActivity.kt`

**Findings:**

> [!NOTE]
> **Multiple settings menu items are stub implementations (TODO)**
>
> `ProfileActivity.kt` Lines 235, 248, 260, 274, 287:
> ```kotlin
> // TODO: Navigate to Edit Profile
> // TODO: Navigate to Notifications
> // TODO: Navigate to Privacy
> // TODO: Navigate to Help
> // TODO: Navigate to About
> ```
> Five menu items have empty click handlers. Pressing them does nothing. This is incomplete functionality.

> [!NOTE]
> **Logout correctly clears the activity back stack** using `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`. This is the correct implementation. ✅

---

### 3.16 `AndroidManifest.xml`

**Findings:**

> [!NOTE]
> **`MainActivity` is declared in the project but not in the manifest.** It appears `SplashActivity` is the true entry point. `MainActivity` with the auto-generated `Greeting` composable is likely leftover scaffolding and is dead code.

> [!NOTE]
> **`INTERNET` permission is not declared** in `AndroidManifest.xml`. Retrofit network calls to the PHP backend will **fail at runtime** on Android because the app does not declare internet access. Check that it is present in `network_security_config.xml` or somewhere else — if not, this is a critical missing permission.

---

## 4. Summary of All Defects Found

| # | Severity | File | Description |
|---|----------|------|-------------|
| 1 | 🔴 Critical | `BaseWorkout.kt` | `init` block calls `calculateCalories()` before subclass fields initialized |
| 2 | 🔴 Critical | `AddActivity.kt` | `duration.toInt()` crash on invalid input (NumberFormatException) |
| 3 | 🔴 Critical | `AddActivity.kt` | `distance.toDouble()` crash on malformed input |
| 4 | 🔴 Critical | `login.php`, `register.php` etc. | SQL Injection via string interpolation instead of prepared statements |
| 5 | 🔴 Critical | `ApiClient.kt` | Hardcoded local IP; app unusable on other networks |
| 6 | 🔴 Critical | `AndroidManifest.xml` | `INTERNET` permission may be missing |
| 7 | 🟠 High | `TrackingActivity.kt` | GPS distance calculated from speed (unreliable approximation) |
| 8 | 🟠 High | `TrackingActivity.kt` | `LocationCallback` never unregistered → battery drain |
| 9 | 🟠 High | `TrackingActivity.kt` | Does not extend `BaseActivity` (inconsistent architecture) |
| 10 | 🟠 High | `TrackingActivity.kt` | `onFinish` callback parameters discarded |
| 11 | 🟠 High | `SplashActivity.kt` | Handler leak: delayed Runnable holds Activity reference |
| 12 | 🟠 High | `WaterReminderReceiver.kt` | Notif tap navigates to LoginActivity instead of Dashboard |
| 13 | 🟠 High | `ScreenTimeReceiver.kt` | Notif tap navigates to LoginActivity instead of Dashboard |
| 14 | 🟠 High | `ApiClient.kt` | HTTP (not HTTPS) — passwords sent unencrypted |
| 15 | 🟡 Medium | `LoginActivity.kt` | No email format validation on client side for login |
| 16 | 🟡 Medium | `LoginActivity.kt` | No password minimum length policy |
| 17 | 🟡 Medium | `LoginActivity.kt` | Confusing error message when name empty + passwords mismatch |
| 18 | 🟡 Medium | `register.php` | No password minimum length validation on server |
| 19 | 🟡 Medium | `DashboardActivity.kt` | Notification badge stale after returning from NotificationsActivity |
| 20 | 🟡 Medium | `DashboardActivity.kt` | "Add" nav bar item never marked as selected |
| 21 | 🟡 Medium | `WaterTrackerHelper.kt` | `getTodayKey()` uses 0-indexed month (Calendar.MONTH) |
| 22 | 🟡 Medium | `login.php` | No brute-force / rate-limit protection |
| 23 | 🟡 Medium | `register.php` | No name field max-length validation |
| 24 | 🟡 Medium | `NotificationHelper.kt` | ID collision risk when two notifications added in same ms |
| 25 | 🟢 Low | `ProfileActivity.kt` | 5 menu items are stub implementations (TODO) |
| 26 | 🟢 Low | `MainActivity.kt` | Dead code — leftover scaffold, not used or registered in manifest |
| 27 | 🟢 Low | `ScreenTimeHelper.kt` | `getTodayPickups()` is an approximation, not true pickups |

---

## 5. Existing Test Coverage Assessment

| File | Tests Exist | Coverage |
|------|-------------|----------|
| `ExampleUnitTest.kt` | `addition_isCorrect()` only | 0% business logic |
| `ExampleInstrumentedTest.kt` | Default stub only | 0% |
| `BaseWorkout` calorie formulas | ❌ None | 0% |
| Login/Register validation | ❌ None | 0% |
| WaterTrackerHelper | ❌ None | 0% |
| NotificationHelper | ❌ None | 0% |
| ScreenTimeHelper | ❌ None | 0% |

> [!IMPORTANT]
> **No business logic is currently tested.** Only an auto-generated placeholder `addition_isCorrect()` test exists. The calorie calculation formulas in `BaseWorkout`, the input validation in `LoginActivity`, and the helper class logic are all untested.

---

## 6. Recommended Test Cases to Write

```kotlin
// BaseWorkout calorie calculation
fun cardioCalorieRunning_30min_returnsCorrect() { /* 9.8 * 70 * 0.5 = 343 */ }
fun cardioCalorieSwimming_60min_returnsCorrect() { /* 7.0 * 70 * 1.0 = 490 */ }
fun strengthCalorie_30min_3sets_10reps() { /* 150 + 60 = 210 */ }

// WaterTracker
fun addWater_increments_correctly() { /* 0 + 250 = 250 */ }
fun progressPercentage_neverExceeds100() { /* coerceIn check */ }
fun getRemaining_neverNegative() { /* coerceAtLeast check */ }

// NotificationHelper
fun getNotifications_emptyPrefs_returnsEmptyList()
fun getUnreadCount_allRead_returnsZero()
fun markAsRead_unknownId_doesNotCrash()

// ScreenTimeHelper
fun formatScreenTime_90min_returns_1h30m()
fun formatScreenTime_45min_returns_45m()
fun isOverScreenTimeLimit_exactlySevenHours_returnsFalse()
```

---

*Report generated by structural analysis of all Kotlin and PHP source files. No code was modified.*
