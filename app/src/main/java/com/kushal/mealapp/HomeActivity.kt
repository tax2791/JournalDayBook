@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.kushal.mealapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.kushal.mealapp.ui.MealPieChart1
import database.MealActivity
import database.MealViewModel
import database.MealViewModelFactory
import database.MyApp
import kotlinx.coroutines.launch
import org.json.JSONObject
import users.UserDetailsActivity

@Suppress("DEPRECATION")
class HomeActivity : ComponentActivity() {

    private lateinit var mealViewModel: MealViewModel
    private lateinit var appUpdateManager: AppUpdateManager
    private val isUpdateAvailableState = mutableStateOf(false)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdate()

        MobileAds.initialize(this) {
            println("✅ AdMob initialized successfully")
        }

        val mealDao = MyApp.getDatabase(applicationContext).mealDao()
        mealViewModel =
            ViewModelProvider(this, MealViewModelFactory(mealDao))[MealViewModel::class.java]

        setContent {
            val isUpdateAvailable by remember { isUpdateAvailableState }

            HomeScreen(
                mealViewModel = mealViewModel,
                isUpdateAvailable = isUpdateAvailable,
                onTriggerUpdate = { triggerAppUpdate() },
                onNavigate = { destination -> navigateTo(destination) },
                onRequireLogin = { redirectActivity ->
                    val intent = Intent(this, LoginComposeActivity::class.java)
                    intent.putExtra("redirectActivity", redirectActivity)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                finish()
            }
        }
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                isUpdateAvailableState.value = true
                if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.IMMEDIATE,
                            this,
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun triggerAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "Opening Play Store for update...", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Checking for latest updates...", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "App is up to date or update check unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val UPDATE_REQUEST_CODE = 1001
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() ==
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    private fun <T> navigateTo(destination: Class<T>) {
        startActivity(Intent(this, destination))
    }
}

data class HomeButton(
    val text: String,
    val iconEmoji: String,
    val color: Color,
    val bgColor: Color,
    val onClick: () -> Unit
)

@Composable
fun BannerAdView() {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }
    val isProUser = sharedPrefs.getBoolean("isProVersion", false)

    // PRO members get a 100% ad-free experience across the entire app
    if (isProUser) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = {
                AdView(it).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-8250990399942328/4059345375"

                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.i("AdMob", "✅ Banner Ad loaded successfully!")
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.e("AdMob", "❌ Banner Ad failed: ${adError.message}")
                        }

                        override fun onAdOpened() {
                            Log.i("AdMob", "ℹ️ Banner Ad opened.")
                        }

                        override fun onAdClicked() {
                            Log.i("AdMob", "🖱️ Banner Ad clicked.")
                        }

                        override fun onAdClosed() {
                            Log.i("AdMob", "ℹ️ Banner Ad closed.")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

@Composable
fun LoginLogoutButton() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val token = prefs.getString("sessionId", null) ?: prefs.getString("sessionId1", null)
    var isLoggedIn by remember { mutableStateOf(!token.isNullOrEmpty()) }

    TextButton(
        onClick = {
            if (isLoggedIn) {
                prefs.edit { clear() }
                isLoggedIn = false
                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, LoginComposeActivity::class.java)
                context.startActivity(intent)
            }
        }
    ) {
        Text(if (isLoggedIn) "Logout" else "Login")
    }
}

@Composable
fun ForgotPassword(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun sendOtp() {
        if (username.isEmpty() || email.isEmpty()) {
            showToast("Please enter both username and email")
            return
        }

        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.api.sendOtp(username, email)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    showToast(body.message)
                    if (body.status.equals("success", ignoreCase = true)) {
                        step = 2
                    }
                } else {
                    showToast("Failed to send OTP: ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun verifyOtpAndReset() {
        if (otp.isEmpty() || newPassword.isEmpty() || email.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.api.resetPassword(username, email, otp, newPassword)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    showToast(body.message)
                    if (body.status.equals("success", ignoreCase = true)) {
                        onDismiss()
                    }
                } else {
                    showToast("Failed to reset password: ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Forgot Password" else "Reset Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (step == 1) {
                    Text("Enter your username and registered email to receive an OTP:")
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Enter the OTP sent to your email and your new password:")
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            val description = if (passwordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (step == 1) sendOtp() else verifyOtpAndReset() }) {
                Text(if (step == 1) "Send OTP" else "Reset Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupForm(onSignupSuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var messName by remember { mutableStateOf("") }
    var selectedEntity by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showMoreFields by remember { mutableStateOf(false) }

    var gstin by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var otherDetails by remember { mutableStateOf("") }

    val entityOptions = listOf(
        "Individual",
        "Firm",
        "Group",
        "Institution",
        "Shop",
        "Other"
    )

    // Regex for username validation: 6-15 chars, lowercase letters, numbers, _, .
    val usernameRegex = remember { Regex("^(?![._])[a-z0-9._]{6,15}(?<![._])$") }

    val isUsernameValid = username.isEmpty() || usernameRegex.matches(username)
    val isEmailValid = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isEmpty() || password.length >= 6
    val isMobileValid = mobile.isEmpty() || mobile.length == 10

    fun submitForm() {
        if (selectedEntity.isBlank()) {
            Toast.makeText(context, "Please select an entity type", Toast.LENGTH_SHORT).show()
            return
        }
        if (messName.isBlank()) {
            Toast.makeText(context, "Mess / Organisation Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (username.isBlank() || !usernameRegex.matches(username)) {
            Toast.makeText(
                context,
                "Username must be 6–15 lowercase letters, numbers, '.' or '_'",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        if (mobile.isNotEmpty() && mobile.length != 10) {
            Toast.makeText(context, "Mobile number must be 10 digits", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://legalcount.in/meal/signup.php"
        isLoading = true

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                isLoading = false
                try {
                    val jsonResponse = JSONObject(response)
                    val message = jsonResponse.getString("message")
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                    if (message.contains("success", ignoreCase = true)) {
                        val sharedPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().apply {
                            putString("username", username)
                            putString("messname", messName)
                            putString("invoice_messname", messName)
                            putString("email", email)
                            putString("gstin", gstin)
                            putString("pan", pan)
                            putString("address", address)
                            putString("mobile", mobile)
                            putString("other_details", otherDetails)
                            putBoolean("isLoggedIn", true)
                        }.commit()

                        messName = ""
                        selectedEntity = ""
                        username = ""
                        password = ""
                        email = ""
                        gstin = ""
                        pan = ""
                        address = ""
                        mobile = ""
                        otherDetails = ""
                        showMoreFields = false
                        onSignupSuccess()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Error parsing server response", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener {
                isLoading = false
                Toast.makeText(context, "Failed to connect to server", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): Map<String, String> = hashMapOf(
                "messName" to messName,
                "entityType" to selectedEntity,
                "username" to username,
                "password" to password,
                "email" to email,
                "gstin" to gstin,
                "pan" to pan,
                "address" to address,
                "mobile" to mobile,
                "otherDetails" to otherDetails
            )
        }

        Volley.newRequestQueue(context).add(request)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Register your mess or entity profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Entity Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedEntity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type of Entity *") },
                        placeholder = { Text("Select entity type...") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        entityOptions.forEach { entity ->
                            DropdownMenuItem(
                                text = { Text(entity) },
                                onClick = {
                                    selectedEntity = entity
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. Name
            OutlinedTextField(
                value = messName,
                onValueChange = { messName = it },
                label = { Text("Mess / Organisation Name *") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().trim() },
                label = { Text("Username *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = !isUsernameValid,
                supportingText = {
                    Text(
                        text = if (!isUsernameValid) "6–15 lowercase chars, numbers, '.' or '_'"
                        else "6–15 lowercase letters, numbers, '_' or '.'",
                        color = if (!isUsernameValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                isError = !isPasswordValid,
                supportingText = {
                    if (!isPasswordValid) {
                        Text("At least 6 characters required", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                }
            )

            // 5. Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email Address *") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = !isEmailValid,
                supportingText = {
                    if (!isEmailValid) {
                        Text("Enter a valid email address", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Additional Details Toggle Card
            Card(
                onClick = { showMoreFields = !showMoreFields },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Additional Details (Optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = showMoreFields,
                        onCheckedChange = { showMoreFields = it }
                    )
                }
            }

            // Optional Fields Block
            AnimatedVisibility(visible = showMoreFields) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase().trim() },
                        label = { Text("GSTIN") },
                        leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it.uppercase().trim() },
                        label = { Text("PAN Number") },
                        leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) mobile = it
                        },
                        label = { Text("Mobile Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        isError = !isMobileValid,
                        supportingText = {
                            if (!isMobileValid) Text("Mobile number must be 10 digits", color = MaterialTheme.colorScheme.error)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = otherDetails,
                        onValueChange = { otherDetails = it },
                        label = { Text("Other Details") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Submit Button
            Button(
                onClick = { scope.launch { submitForm() } },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlashingUpdateBanner(
    onUpdateClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "UpdateFlashTransition")

    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF00B0FF),
        targetValue = Color(0xFF80D8FF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlashColorAnimation"
    )

    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlashAlphaAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onUpdateClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = animatedColor.copy(alpha = animatedAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🚀",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "New Update Available!",
                        color = Color(0xFF002B49),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Tap to update to the latest version",
                        color = Color(0xFF004D40),
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onUpdateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF002B49)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "UPDATE",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun OfflineActionCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    badgeColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text("➔", fontSize = 14.sp, color = badgeColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OnlineGridCard(
    title: String,
    iconEmoji: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HomeScreen(
    mealViewModel: MealViewModel,
    isUpdateAvailable: Boolean = false,
    onTriggerUpdate: () -> Unit = {},
    onNavigate: (Class<*>) -> Unit,
    onRequireLogin: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCalculator by remember { mutableStateOf(false) }
    var showEMI by remember { mutableStateOf(false) }
    var showOfflineSection by remember { mutableStateOf(true) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val sharedPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val token = sharedPrefs.getString("sessionId", null) ?: sharedPrefs.getString("sessionId1", null)
    val isLoggedIn = !token.isNullOrEmpty()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF8F9FA),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF3F51B5), Color(0xFF0288D1), Color(0xFF26A69A))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📘", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Journal DayBook",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Quick Actions & Tools",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NAVIGATION MENU",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                if (isUpdateAvailable) {
                    NavigationDrawerItem(
                        label = { Text("Update Available!", fontWeight = FontWeight.Bold, color = Color(0xFF002B49)) },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF80D8FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🚀", fontSize = 16.sp)
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onTriggerUpdate()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color(0xFF00B0FF).copy(alpha = 0.25f)),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                }

                NavigationDrawerItem(
                    label = { Text("Backup to Drive", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE3F2FD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF1976D2))
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(GoogleSignInActivity::class.java)
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("My Expenses", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFFF3E0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFF57C00))
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(MealActivity::class.java)
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                NavigationDrawerItem(
                    label = { Text("Calculator", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE8F5E9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF388E3C))
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        FloatingCalculatorState.show()
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Converter", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF3E5F5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF7B1FA2))
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showEMI = true
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFE0F2F1), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ App Mode: ${if (showOfflineSection) "Offline" else "Online"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00695C)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6F9))
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = Color(0xFFF4F6F9),
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            text = "Journal DayBook",
                            color = Color(0xFF002B49),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(40.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF0288D1), Color(0xFF26A69A))
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { scope.launch { drawerState.open() } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Side Menu",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { FloatingCalculatorState.show() }) {
                            Text("🧮", fontSize = 18.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F6F9))
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BannerAdView()
                }
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp)
            ) {
                // Flashing Light Blue Update Banner
                if (isUpdateAvailable) {
                    FlashingUpdateBanner(onUpdateClick = onTriggerUpdate)
                }

                // Professional Segmented Control (Mode Switcher)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (showOfflineSection) Color(0xFF0288D1) else Color.Transparent)
                                .clickable { showOfflineSection = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📱 ", fontSize = 14.sp)
                                Text(
                                    "Offline Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (showOfflineSection) Color.White else Color(0xFF555555)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!showOfflineSection) Color(0xFF0288D1) else Color.Transparent)
                                .clickable { showOfflineSection = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐 ", fontSize = 14.sp)
                                Text(
                                    "Online Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (!showOfflineSection) Color.White else Color(0xFF555555)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!showOfflineSection) {
                    // Online Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val username = sharedPrefs.getString("username", "User")
                        if (!isLoggedIn) {
                            SignupForm()
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(context, UserDetailsActivity::class.java)
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Welcome back,", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        Text("$username!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Text("Account ➔", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Bottom Row: Forgot Password + Login/Logout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showForgotDialog = true }) {
                                Text("Forgot Password?", color = Color(0xFF1E88E5), fontWeight = FontWeight.SemiBold)
                            }
                            LoginLogoutButton()
                        }

                        // Online buttons grid below form
                        val onlineButtons = listOf(
                            HomeButton("+ Expenses", "➕", Color(0xFFEB940F), Color(0xFFFFF3E0)) {
                                if (isLoggedIn) onNavigate(MainActivity::class.java)
                                else onRequireLogin("MainActivity")
                            },
                            HomeButton("+ Member", "👥", Color.Gray, Color(0xFFF1F5F9)) {
                                if (isLoggedIn) onNavigate(NameActivityAPI::class.java)
                                else onRequireLogin("NameActivityAPI")
                            },
                            HomeButton("All Reports", "📄", Color.Green, Color(0xFFE8F5E9)) {
                                if (isLoggedIn) onNavigate(Details::class.java)
                                else onRequireLogin("Details")
                            },
                            HomeButton("Notes", "📝", Color.Red, Color(0xFFFFEBEE)) {
                                if (isLoggedIn) onNavigate(ChatActivity::class.java)
                                else onRequireLogin("ChatActivity")
                            },
                            HomeButton("Calculator", "🧮", Color(0xFFADD8E6), Color(0xFFE0F7FA)) { FloatingCalculatorState.show() },
                            HomeButton("Converter", "📊", Color(0xFF03A9F4), Color(0xFFF3E5F5)) { showEMI = true }
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            items(onlineButtons) { btn ->
                                OnlineGridCard(
                                    title = btn.text,
                                    iconEmoji = btn.iconEmoji,
                                    bgColor = btn.bgColor,
                                    onClick = btn.onClick
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HelpAndSuggestionCard()
                    }
                } else {
                    // Offline Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OfflineActionCard(
                            title = "Backup to Drive",
                            subtitle = "Automatic Cloud Synchronization",
                            iconEmoji = "☁️",
                            badgeColor = Color(0xFF1976D2),
                            bgColor = Color(0xFFE3F2FD),
                            onClick = { onNavigate(GoogleSignInActivity::class.java) }
                        )

                        OfflineActionCard(
                            title = "My Expenses Dashboard",
                            subtitle = "Manage Offline Members & Expenditures",
                            iconEmoji = "💸",
                            badgeColor = Color(0xFFF57C00),
                            bgColor = Color(0xFFFFF3E0),
                            onClick = { onNavigate(MealActivity::class.java) }
                        )

                        OfflineActionCard(
                            title = "Loan Account & Collections",
                            subtitle = "Track Offline Loans Given, Repayments & Reminders",
                            iconEmoji = "🤝",
                            badgeColor = Color(0xFF8E24AA),
                            bgColor = Color(0xFFF3E5F5),
                            onClick = { onNavigate(LoanActivity::class.java) }
                        )

                        OfflineActionCard(
                            title = "Standard Calculator",
                            subtitle = "Quick On-Screen Calculations",
                            iconEmoji = "🧮",
                            badgeColor = Color(0xFF388E3C),
                            bgColor = Color(0xFFE8F5E9),
                            onClick = { FloatingCalculatorState.show() }
                        )

                        OfflineActionCard(
                            title = "PRO Calculator & Converter",
                            subtitle = "EMI, Age & Unit Conversion Tools",
                            iconEmoji = "📊",
                            badgeColor = Color(0xFF7B1FA2),
                            bgColor = Color(0xFFF3E5F5),
                            onClick = { showEMI = true }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val mealList by mealViewModel.allMeals1.observeAsState(emptyList())
                        if (mealList.isNotEmpty()) {
                            MealPieChart1(viewModel = mealViewModel)
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📊", fontSize = 32.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "No Expense Data Yet",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "Add transactions in My Expenses to see pie chart",
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HelpAndSuggestionCard()
                    }
                }
            }
        }
    }

    GlobalCalculatorOverlay()
    if (showEMI) EMICalculatorPage(onBack = { showEMI = false })
    if (showForgotDialog) ForgotPassword(onDismiss = { showForgotDialog = false })
}

@Composable
fun HelpAndSuggestionCard() {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:kushal.dan@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Journal DayBook - Help & Suggestion")
                        putExtra(Intent.EXTRA_TEXT, "Hello Kushal,\n\nI have the following query / suggestion regarding Journal DayBook:\n\n")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email via..."))
                } catch (e: Exception) {
                    Toast.makeText(context, "No email client found on device", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Line 1: Header Title & Support Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Help & Suggestions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF002B49)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = "Support",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Line 2: Subtitle Description
            Text(
                text = "Have questions, feedback, or feature ideas? Send your thoughts directly.",
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1
            )

            // Line 3: Direct Email Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text("✉️", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Send Email to Journal DayBook",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}
