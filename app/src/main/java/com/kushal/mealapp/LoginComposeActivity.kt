package com.kushal.mealapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@LoginComposeActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // ✅ Get redirect target (which activity called login)
        val redirectActivity = intent.getStringExtra("redirectActivity")


        // ✅ If already logged in, redirect immediately
        val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("sessionId", null)
        if (token != null) {
            redirectToActivity(redirectActivity)
            finish()
            return
        }

        // ✅ Otherwise, show login UI
        setContent {
            LoginScreen(
                onLoginSuccess = {
                    redirectToActivity(redirectActivity)
                    finish()
                }
            )
        }
    }

    // ✅ Normal function (not composable)
    private fun redirectToActivity(activityName: String?) {
        val intent = when (activityName) {
            "Details" -> Intent(this, Details::class.java)
            "ChatActivity" -> Intent(this, ChatActivity::class.java)
            "NameActivityAPI" -> Intent(this, NameActivityAPI::class.java)
            "MainActivity" -> Intent(this, MainActivity::class.java)
            else -> Intent(this, HomeActivity::class.java)
        }
        startActivity(intent)
        finish() // optional, close login activity after redirect
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val enableButton = username.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))


        var passwordVisible by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )


        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    isLoading = true
                    loginUser(context, username, password, onLoginSuccess) {
                        isLoading = false
                    }
                },
                enabled = enableButton,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
}

// ✅ Handles actual login call
private fun loginUser(
    context: Context,
    username: String,
    password: String,
    onLoginSuccess: () -> Unit,
    onComplete: () -> Unit
) {
    val apiService = RetrofitInstance.api
    val loginRequest = LoginRequest(username, password)
    Log.d("LoginCompose", "API call started with username: $username")

    apiService.loginUserWithJson(loginRequest).enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            onComplete()
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null && loginResponse.success) {
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    saveLoginDetails(
                        context,
                        loginResponse.token,
                        loginResponse.username,
                        password,
                        loginResponse.sessionId1
                    )
                    onLoginSuccess()
                } else {
                    Toast.makeText(
                        context,
                        loginResponse?.message ?: "Invalid credentials",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            onComplete()
            Toast.makeText(context, "Login Failed: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

private fun saveLoginDetails(
    context: Context,
    token: String?,
    username: String?,
    password: String,
    sessionId1: String?
) {
    if (token != null && username != null) {
        val sharedPreferences = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("sessionId", token)
            putString("username", username)
            putString("password", password)
            putString("sessionId1", sessionId1)
            putBoolean("isLoggedIn", true)
            apply()
        }
        Log.d("LoginCompose", "Login details saved in SharedPreferences")
        Log.d("LoginCompose", "Token: $token")
    } else {
        Log.e("LoginCompose", "Token or username is null; not saved")
    }
}
