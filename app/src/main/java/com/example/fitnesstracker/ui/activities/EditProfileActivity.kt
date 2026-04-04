package com.example.fitnesstracker.ui.activities


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.UpdateProfileRequest
import com.example.fitnesstracker.data.network.UpdateProfileResponse
import com.example.fitnesstracker.data.network.User
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme
import com.example.fitnesstracker.utils.BaseActivity
import com.example.fitnesstracker.utils.CommonHeader

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = getUserId()
        val currentName = getUserName()
        val currentEmail = getUserEmail()

        setContent {
            FitnesstrackerTheme {
                EditProfileScreen(
                    userId = userId,
                    initialName = currentName,
                    initialEmail = currentEmail,
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
    onBack: () -> Unit,
    onUpdateSuccess: (User) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var isLoading by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
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

            // Name field
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

            // Email Field
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

            // Save Button
            Button(
                onClick = {
                    if (name.isNotEmpty() && email.isNotEmpty()) {
                        isLoading = true
                        val request = UpdateProfileRequest(userId, name, email)
                        ApiClient.apiService.updateProfile(request).enqueue(object : Callback<UpdateProfileResponse> {
                            override fun onResponse(call: Call<UpdateProfileResponse>, response: Response<UpdateProfileResponse>) {
                                isLoading = false
                                val body = response.body()
                                if (body != null && body.success && body.user != null) {
                                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                    onUpdateSuccess(body.user)
                                } else {
                                    Toast.makeText(context, body?.message ?: "Update failed", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                                isLoading = false
                                val errorMsg = when (t) {
                                    is java.net.ConnectException -> "Cannot connect to server. Check your network."
                                    is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                                    else -> "Error: ${t.localizedMessage ?: "Unknown error"}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        })
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