package com.example.fitnesstracker.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.User
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme
import com.example.fitnesstracker.ui.viewmodel.FitnessViewModel
import com.example.fitnesstracker.ui.viewmodel.ViewModelFactory
import com.example.fitnesstracker.utils.CommonHeader

class EditProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getIntExtra("USER_ID", 0)
        val currentName = intent.getStringExtra("USER_NAME") ?: ""
        val currentEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        val repository = DataRepository(ApiClient.apiService)
        val factory = ViewModelFactory(application, repository)

        setContent {
            FitnesstrackerTheme {
                val viewModel: FitnessViewModel = viewModel(factory = factory)
                EditProfileScreen(
                    userId = userId,
                    initialName = currentName,
                    initialEmail = currentEmail,
                    viewModel = viewModel,
                    onBack = { finish() },
                    onUpdateSuccess = { updatedUser ->
                        val resultIntent = Intent()
                        resultIntent.putExtra("UPDATED_NAME", updatedUser.name)
                        resultIntent.putExtra("UPDATED_EMAIL", updatedUser.email)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    userId: Int,
    initialName: String,
    initialEmail: String,
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    onUpdateSuccess: (User) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val updateResult by viewModel.updateResult.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    // Observe profile so we can pre-fill fields if the Intent extras were empty
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // If the caller did not supply name/email (profile wasn't yet loaded in ProfileActivity),
    // trigger a fresh fetch so the fields can be populated automatically.
    LaunchedEffect(userId) {
        if (initialName.isBlank() || initialEmail.isBlank()) {
            viewModel.fetchProfile(userId)
        }
    }

    // Whenever the ViewModel delivers a fresh profile, update the local text fields.
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            if (name.isBlank()) name = profile.name
            if (email.isBlank()) email = profile.email
        }
    }

    LaunchedEffect(updateResult) {
        updateResult?.onSuccess { response ->
            if (response.success && response.user != null) {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                onUpdateSuccess(response.user)
                viewModel.clearResults()
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearResults()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            CommonHeader(
                title = "Edit Profile",
                subtitle = "Update your account info",
                onBackClick = onBack,
                darkPurple = backgroundColor
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedTextColor = onSurfaceColor,
                    unfocusedTextColor = onSurfaceColor
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedTextColor = onSurfaceColor,
                    unfocusedTextColor = onSurfaceColor
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor)
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (name.isNotEmpty() && email.isNotEmpty()) {
                        viewModel.updateProfile(userId, name, email)
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE CHANGES",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}