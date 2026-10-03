package com.kushal.mealapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

        // ✅ Get redirect target (from Intent or SharedPreferences fallback)
        val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val redirectActivity = intent.getStringExtra("redirectActivity")
            ?: sharedPreferences.getString("redirectActivity", null)

        // ✅ If already logged in, redirect immediately
        val existingToken = sharedPreferences.getString("sessionId", null)
            ?: sharedPreferences.getString("sessionId1", null)

        if (!existingToken.isNullOrEmpty()) {
            redirectToActivity(redirectActivity)
            return
        }

        // ✅ Otherwise, show login UI
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginSuccess = {
                        redirectToActivity(redirectActivity)
                    },
                    onGuestContinue = {
                        startActivity(Intent(this@LoginComposeActivity, HomeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    // ✅ Normal function (not composable)
    private fun redirectToActivity(activityName: String?) {
        // Clear stored redirectActivity after using it
        getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit().remove("redirectActivity").commit()

        val intent = when (activityName) {
            "Details" -> Intent(this, Details::class.java)
            "ChatActivity" -> Intent(this, ChatActivity::class.java)
            "NameActivityAPI" -> Intent(this, NameActivityAPI::class.java)
            "MainActivity" -> Intent(this, MainActivity::class.java)
            else -> Intent(this, HomeActivity::class.java)
        }
        startActivity(intent)
        finish() // close login activity after redirect
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGuestContinue: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val enableButton = username.isNotBlank() && password.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branding Header
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = "App Logo",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Sign in to manage your meal records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Username Icon"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password Icon"
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (enableButton && !isLoading) {
                                keyboardController?.hide()
                                isLoading = true
                                loginUser(context, username, password, onLoginSuccess) {
                                    isLoading = false
                                }
                            }
                        }
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action Button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        isLoading = true
                        loginUser(context, username, password, onLoginSuccess) {
                            isLoading = false
                        }
                    },
                    enabled = enableButton && !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onGuestContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Back to Home",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ✅ Handles actual login call
private fun loginUser(
    context: Context,
    usernameInput: String,
    passwordInput: String,
    onLoginSuccess: () -> Unit,
    onComplete: () -> Unit
) {
    val apiService = RetrofitInstance.api
    val loginRequest = LoginRequest(usernameInput, passwordInput)
    Log.d("LoginCompose", "API call started with username: $usernameInput")

    apiService.loginUserWithJson(loginRequest).enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            onComplete()
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null && loginResponse.success) {
                    val saved = saveLoginDetails(
                        context = context,
                        token = loginResponse.token,
                        responseUsername = loginResponse.username,
                        inputUsername = usernameInput,
                        password = passwordInput,
                        sessionId1 = loginResponse.sessionId1
                    )
                    if (saved) {
                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            "Login Failed: Invalid session token received from server",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
    responseUsername: String?,
    inputUsername: String,
    password: String,
    sessionId1: String?
): Boolean {
    val effectiveToken = when {
        !token.isNullOrBlank() -> token.trim()
        !sessionId1.isNullOrBlank() -> sessionId1.trim()
        else -> null
    }

    val effectiveUsername = when {
        !responseUsername.isNullOrBlank() -> responseUsername.trim()
        !inputUsername.isNullOrBlank() -> inputUsername.trim()
        else -> null
    }

    if (effectiveToken != null && effectiveUsername != null) {
        val sharedPreferences = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val success = sharedPreferences.edit().apply {
            putString("sessionId", effectiveToken)
            putString("username", effectiveUsername)
            putString("password", password)
            putString("sessionId1", if (!sessionId1.isNullOrBlank()) sessionId1.trim() else effectiveToken)
            putBoolean("isLoggedIn", true)
        }.commit() // ✅ MUST use commit() for synchronous write before redirecting!

        Log.d("LoginCompose", "Login details saved ($success) in SharedPreferences. Token: $effectiveToken, User: $effectiveUsername")
        return success
    } else {
        Log.e("LoginCompose", "Token or username is null; effectiveToken=$effectiveToken, effectiveUsername=$effectiveUsername")
        return false
    }
}
