# Tracker for Fitness - Refactoring & Enhancement Plan

Following your request to rename the project to "Tracker for Fitness," restructure the folders, improve the codebase architecture, and add a **"Remember Me"** feature while ensuring the app continues to function correctly, I have updated the implementation plan.

## User Review Required

> [!IMPORTANT]
> - **IP Address Config:** Given we are refactoring, I will configure the API `BASE_URL` to default to `10.0.2.2` (the standard Android Emulator localhost IP). Please confirm if you will be using a physical device or an emulator to test this.
> - **Architecture Shift:** The new folder structure will dramatically reorganize the files into standard modern Android packages (`data`, `ui`, `utils`). This is extensive but necessary for clean code. Does the proposed structure below look good?

## Proposed Changes

---
### Phase 1: Project Renaming & Configuration

#### [MODIFY] [strings.xml](file:///d:/L5DC/project/MAD/fitnesstracker/app/src/main/res/values/strings.xml)
- Change `<string name="app_name">fitnesstracker</string>` to `"Tracker for Fitness"`.

#### [MODIFY] [AndroidManifest.xml](file:///d:/L5DC/project/MAD/fitnesstracker/app/src/main/AndroidManifest.xml)
- Add the `INTERNET` permission to fix missing internet crashes.
- Update activity `android:name` paths to reflect the new folder structure (e.g., `.ui.activities.SplashActivity`).

---
### Phase 2: Folder & Code Structure Refactoring

We will organize the root `com.example.fitnesstracker` package into a standard Clean Architecture-like folder structure. This addresses the "line by line checking and code writing structure" request by separating concerns:

1. **`data/` (Networking & Responses)**
   - Move `ApiClient.kt` and `ApiService.kt` here.
   - Update `BASE_URL`.
2. **`models/` (Data Entities & OOP Logic)**
   - Move `BaseWorkout.kt`, `CardioWorkout`, `StrengthWorkout` here.
   - **Fix:** Update `_caloriesBurned` logic to prevent the initialization defect.
3. **`utils/` (Helpers & Base Logic)**
   - Move `ScreenTimeHelper.kt`, `WaterTrackerHelper.kt`, `NotificationHelper.kt`.
   - **Fix:** Address the 0-indexed month bug inside `WaterTrackerHelper`.
4. **`receivers/` (Background Services)**
   - Move `WaterReminderReceiver.kt` and `ScreenTimeReceiver.kt`, fixing their Intent navigation.
5. **`ui/activities/` (Screens & View Logic)**
   - Move all Activity files (`SplashActivity`, `LoginActivity`, `DashboardActivity`, `AddActivity`, `TrackingActivity`, `ProfileActivity`, `StatisticsActivity`).
   - Fix `TrackingActivity` inheritance and GPS distance.
   - Fix `AddActivity` number format bounds checking.

---
### Phase 3: Backend Security Integrity Fixes

#### [MODIFY] `phps/login.php`, `register.php`, `save_activity.php`, `get_activities.php`
- I will execute a line-by-line replacement of interpolated `$conn->query()` SQL strings with secure parameterized statements (`$stmt = $conn->prepare()`) to eliminate SQL Injection faults from the White Box report.

---
### Phase 4: Session State & "Remember Me"

#### [MODIFY] [LoginActivity.kt](file:///d:/L5DC/project/MAD/fitnesstracker/app/src/main/java/com/example/fitnesstracker/LoginActivity.kt)
- Add a "Remember Me" Checkbox directly below the Password interface.
- On successful login, save `USER_ID`, `USER_NAME`, `USER_EMAIL`, and `REMEMBER_ME=true` boolean state into SharedPreferences.

#### [MODIFY] [SplashActivity.kt](file:///d:/L5DC/project/MAD/fitnesstracker/app/src/main/java/com/example/fitnesstracker/SplashActivity.kt)
- After 2.5 seconds, check SharedPreferences. If `REMEMBER_ME` is true and a valid User ID exists, navigate *directly* to the `DashboardActivity` (bypassing the Login screen entirely).

#### [MODIFY] [ProfileActivity.kt](file:///d:/L5DC/project/MAD/fitnesstracker/app/src/main/java/com/example/fitnesstracker/ProfileActivity.kt)
- Ensure the logout function purges the `REMEMBER_ME` flag from SharedPreferences so the user stays logged out.

---
### Phase 5: UI Design System & New Features

#### [NEW Feature] "Yoga" Workout
- Integrated into `AddActivity` UI Selection Grid and mapped seamlessly through `BaseWorkout` via Polymorphism metrics.

#### [MODIFY] Design Polish
- Apply a deep minimalist aesthetic (deep deep purple/black `#121212`), high-contrast modern typography, and glassmorphic card overlays similar to Strava or Apple Fitness.

## Verification Plan
1. Launch the app and verify the splash screen shows **"Tracker for Fitness"**.
2. Perform a fresh Login, check "Remember Me", close the app fully, and reopen it to verify it skips Login.
3. Test a logout safely clears the auto-login flag.
4. Try SQL injection vectors to confirm the PHP endpoints are hardened.
5. Create a "Yoga" activity to confirm new metrics are stable.
