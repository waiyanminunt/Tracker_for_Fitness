package com.example.fitnesstracker.ui.activities

import com.example.fitnesstracker.data.network.ApiClient
import com.example.fitnesstracker.data.network.LoginRequest
import com.example.fitnesstracker.data.network.LoginResponse
import com.example.fitnesstracker.data.network.RegisterRequest
import com.example.fitnesstracker.data.network.RegisterResponse
import com.example.fitnesstracker.data.network.User
import com.example.fitnesstracker.R
import com.example.fitnesstracker.ui.theme.FitnesstrackerTheme

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnesstrackerTheme {
                LoginScreen(
                    onLoginSuccess = { user, rememberMe ->
                        val sharedPrefs = getSharedPreferences("FitnessTrackerPrefs", android.content.Context.MODE_PRIVATE)
                        if (rememberMe) {
                            // Save session only when Remember Me is checked
                            with(sharedPrefs.edit()) {
                                putInt("USER_ID", user.id)
                                putString("USER_NAME", user.name)
                                putString("USER_EMAIL", user.email)
                                apply()
                            }
                        } else {
                            // Clear any previously saved session
                            sharedPrefs.edit().clear().apply()
                        }
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.putExtra("USER_ID", user.id)
                        intent.putExtra("USER_NAME", user.name)
                        intent.putExtra("USER_EMAIL", user.email)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (User, Boolean) -> Unit) {
    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(primaryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.heart_pulse),
                contentDescription = "App Logo",
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Name
        Text(
            text = "Fitness Tracker",
            color = onSurfaceColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = if (isLoginMode) "Welcome Back!" else "Create Account",
            color = primaryColor,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Name field (only for register)
        if (!isLoginMode) {
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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

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
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.Gray) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = primaryColor
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor,
                focusedTextColor = onSurfaceColor,
                unfocusedTextColor = onSurfaceColor
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Confirm Password (only for Register mode)
        if (!isLoginMode) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", color = Color.Gray) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedTextColor = onSurfaceColor,
                    unfocusedTextColor = onSurfaceColor
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Remember Me checkbox (only shown in Login mode)
        if (isLoginMode) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = primaryColor,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Text(
                    text = "Remember Me",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Login / Register Button
        Button(
            onClick = {
                if (isLoginMode) {
                    // Login
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        val request = LoginRequest(email, password)
                        ApiClient.apiService.login(request).enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                isLoading = false
                                val body = response.body()
                                if (body != null && body.success && body.user != null) {
                                    Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(body.user, rememberMe)
                                } else {
                                    Toast.makeText(context, body?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                isLoading = false
                                val errorMsg = when (t) {
                                    is java.net.ConnectException -> "Cannot connect to server. Check your network and IP address."
                                    is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                                    else -> "Error: ${t.localizedMessage}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        })
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Register
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword) {
                        isLoading = true
                        val request = RegisterRequest(name, email, password)
                        ApiClient.apiService.register(request).enqueue(object : Callback<RegisterResponse> {
                            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                                isLoading = false
                                val body = response.body()
                                if (body != null && body.success) {
                                    Toast.makeText(context, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                                    isLoginMode = true
                                    password = ""
                                    confirmPassword = ""
                                } else {
                                    Toast.makeText(context, body?.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                                isLoading = false
                                val errorMsg = when (t) {
                                    is java.net.ConnectException -> "Cannot connect to server. Check your network and IP address."
                                    is java.net.SocketTimeoutException -> "Connection timed out. Server might be slow."
                                    else -> "Error: ${t.localizedMessage}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        })
                    } else {
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                CircularProgressIndicator(
                    color = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = if (isLoginMode) "LOGIN" else "REGISTER",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Login / Register
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoginMode) "Don't have an account?" else "Already have an account?",
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(
                    text = if (isLoginMode) "Register" else "Login",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}