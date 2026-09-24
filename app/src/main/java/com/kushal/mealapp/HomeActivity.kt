@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.kushal.mealapp


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import database.MealViewModel
import database.MealViewModelFactory
import database.MyApp
import com.kushal.mealapp.ui.MealPieChart1
import database.MealActivity
import kotlinx.coroutines.launch
import org.json.JSONObject
import users.UserDetailsActivity

@Suppress("DEPRECATION")
class HomeActivity : ComponentActivity() {

    private lateinit var mealViewModel: MealViewModel
    private lateinit var appUpdateManager: AppUpdateManager

    // Launcher required for IMMEDIATE update
    private val updateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                // User cancelled update → close app (mandatory update)
                finish()
            }
        }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔄 Initialize App Update Manager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdate()

        // ✅ Initialize AdMob only once
        MobileAds.initialize(this) {
            println("✅ AdMob initialized successfully")
        }

        val prefs = getSharedPreferences("meal_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("has_restored_db", false)) {
            MyApp.clearDatabaseInstance()
        }

        val mealDao = MyApp.getDatabase(applicationContext).mealDao()
        mealViewModel =
            ViewModelProvider(this, MealViewModelFactory(mealDao))[MealViewModel::class.java]

        setContent {
            HomeScreen(
                mealViewModel = mealViewModel,
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
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // User cancelled → close app (forced update)
                finish()
            }
        }
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
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


@Composable
fun HomeScreen(
    mealViewModel: MealViewModel,
    onNavigate: (Class<*>) -> Unit,
    onRequireLogin: (String) -> Unit,
) {
    val context = LocalContext.current
    var showCalculator by remember { mutableStateOf(false) }
    var showEMI by remember { mutableStateOf(false) }
    var showOfflineSection by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val sharedPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val isLoggedIn = sharedPrefs.getString("sessionId", null) != null

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFAAA1CD))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color(0xFFAAA1CD)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFAAA1CD))
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Journal DayBook",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF98F5F9),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )

            // Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { showOfflineSection = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showOfflineSection) Color(0xFF03A9F4) else Color.Gray
                    )
                ) { Text("Go To Online") }

                Button(
                    onClick = { showOfflineSection = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOfflineSection) Color(0xFF03A9F4) else Color.Gray
                    )
                ) { Text("Go To Offline") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showOfflineSection) {
                // Online Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    //SignupForm() // Embedded Signup form
                    val username = sharedPrefs.getString("username", "User")
                    if (!isLoggedIn) {
                        SignupForm() // Show only if NOT logged in
                    } else {
                        val context = LocalContext.current

                        Text(
                            text = "Welcome back! $username!",
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    val intent = Intent(context, UserDetailsActivity::class.java)
                                    context.startActivity(intent)
                                },
                            textAlign = TextAlign.Center
                        )

                    }
                    // 🔹 Bottom Row: Forgot Password + Login/Logout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showForgotDialog = true },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("Forgot Password?")
                        }

                        LoginLogoutButton()
                    }


                    // Online buttons grid below form
                    val onlineButtons = listOf(
                        HomeButton("+ Expenses", Color(0xFFEB940F)) {
                            if (isLoggedIn) onNavigate(MainActivity::class.java)
                            else onRequireLogin("MainActivity")
                        },
                        HomeButton("+ Member", Color.Gray) {
                            if (isLoggedIn) onNavigate(NameActivityAPI::class.java)
                            else onRequireLogin("NameActivityAPI")
                        },
                        HomeButton("All Reports", Color.Green) {
                            if (isLoggedIn) onNavigate(Details::class.java)
                            else onRequireLogin("Details")
                        },


                        HomeButton("Notes", Color.Red) {
                            if (isLoggedIn) onNavigate(ChatActivity::class.java)
                            else onRequireLogin("ChatActivity")
                        },

                        HomeButton("Calculator", Color(0xFFADD8E6)) { showCalculator = true },
                        HomeButton("PRO Calculator", Color(0xFF03A9F4)) { showEMI = true },


                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        items(onlineButtons) { btn ->
                            Button(
                                onClick = btn.onClick,
                                colors = ButtonDefaults.buttonColors(containerColor = btn.color),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            ) { Text(btn.text, fontSize = 12.sp, color = Color.Black) }
                        }
                    }
                    // ✅ Always show AdMob banner at the bottom
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sponsored",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        BannerAdView()
                    }
                }
            } else {
                // Offline Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val offlineButtons = listOf(
                        HomeButton("BackUp", Color.Blue) { onNavigate(GoogleSignInActivity::class.java) },
                        HomeButton("My Expenses", Color(0xFFEB940F)) { onNavigate(MealActivity::class.java) },
                        HomeButton("Calculator", Color(0xFFADD8E6)) { showCalculator = true },
                        HomeButton("EMI Calculator", Color(0xFF03A9F4)) { showEMI = true }
                    )

                    offlineButtons.forEach { btn ->
                        Button(
                            onClick = btn.onClick,
                            colors = ButtonDefaults.buttonColors(containerColor = btn.color),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) { Text(btn.text, fontSize = 14.sp, color = Color.Black) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val mealList by mealViewModel.allMeals1.observeAsState(emptyList())
                    if (mealList.isNotEmpty()) {
                        MealPieChart1(viewModel = mealViewModel)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Data Available")
                        }
                    }
                    // ✅ Always show AdMob banner at the bottom
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sponsored",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        BannerAdView()
                    }
                }
            }

            // ✅ Always show AdMob banner at the bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sponsored",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                BannerAdView()
            }
        }
    }

    // Popups
    if (showCalculator) CalculatorPopup(onDismiss = { showCalculator = false })
    if (showEMI) EMICalculatorPage(onBack = { showEMI = false })
    // 🔹 Show Forgot Password Dialog
    if (showForgotDialog) {
        ForgotPassword(onDismiss = { showForgotDialog = false })


        // ✅ Always show Banner Ad at bottom
        Spacer(modifier = Modifier.height(16.dp))
        BannerAdView()

    }
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
        "Establishment",
        "Other"
    )

    fun submitForm() {
        if (messName.isEmpty() || selectedEntity.isEmpty() || username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Error parsing response", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener {
                isLoading = false
                Toast.makeText(context, "Failed to submit data", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): Map<String, String> = hashMapOf(
                "messName" to messName,
                "entityType" to selectedEntity,
                "username" to username,
                "password" to password,
                "email" to email,
                // Optional fields (send empty if not filled)
                "gstin" to gstin,
                "pan" to pan,
                "address" to address,
                "mobile" to mobile,
                "otherDetails" to otherDetails
            )
        }

        Volley.newRequestQueue(context).add(request)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Create Account",
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                // 🔹 Dropdown for Type of Entity
                // 🔹 Dropdown for Type of Entity (Fixed visibility issue)
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            value = selectedEntity,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type of Entity") },
                            placeholder = { Text("Select entity...") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor() // 🟢 Important for correct positioning
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


                // 🔹 Mess Name TextField (Separate)
                OutlinedTextField(
                    value = messName,
                    onValueChange = { messName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 🔹 Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Username must be 6–15 characters, lowercase letters, numbers, underscores, or dots (no starting/ending with . or _)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                // 🔹 Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    }
                )

                // 🔹 Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 🔹 Toggle More Fields
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Checkbox(
                        checked = showMoreFields,
                        onCheckedChange = { showMoreFields = it }
                    )
                    Text("Add more details (optional)")
                }
                if (showMoreFields) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        label = { Text("GSTIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it },
                        label = { Text("PAN Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = {
                            if (it.length <= 10) mobile = it
                        },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = otherDetails,
                        onValueChange = { otherDetails = it },
                        label = { Text("Other Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 🔹 Signup Button
                Button(
                    onClick = { scope.launch { submitForm() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Sign Up")
                }


            }


        }
    }


}

@Composable
fun ForgotPassword(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1 = username+email, 2 = otp+new password
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
            Log.w("ForgotPassword", "Missing username or email")
            return
        }

        scope.launch {
            isLoading = true
            try {
                Log.d("ForgotPassword", "Sending OTP for user=$username email=$email")
                val response = RetrofitClient.api.sendOtp(username, email)
                Log.d("ForgotPassword", "OTP Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("ForgotPassword", "OTP Response Body: ${body.status} - ${body.message}")
                    showToast(body.message)
                    if (body.status.equals("success", ignoreCase = true)) {
                        step = 2
                    }
                } else {
                    Log.e("ForgotPassword", "OTP failed with HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    showToast("Failed to send OTP: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ForgotPassword", "OTP Exception: ${e.message}", e)
                showToast("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun verifyOtpAndReset() {
        if (otp.isEmpty() || newPassword.isEmpty() || email.isEmpty()) {
            showToast("Please fill all fields")
            Log.w("ForgotPassword", "Missing required fields for reset")
            return
        }

        scope.launch {
            isLoading = true
            try {
                Log.d("ForgotPassword", "Verifying OTP and resetting password for $username / $email")
                val response = RetrofitClient.api.resetPassword(username, email, otp, newPassword)
                Log.d("ForgotPassword", "Reset Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("ForgotPassword", "Reset Response Body: ${body.status} - ${body.message}")
                    showToast(body.message)
                    if (body.status.equals("success", ignoreCase = true)) {
                        Log.i("ForgotPassword", "Password reset successful for user=$username")
                        onDismiss()
                    }
                } else {
                    Log.e("ForgotPassword", "Reset failed with HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    showToast("Failed to reset password: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ForgotPassword", "Reset Exception: ${e.message}", e)
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
                    var newPassword by remember { mutableStateOf("") }
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

@Composable
fun LoginLogoutButton() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    var isLoggedIn by remember { mutableStateOf(prefs.getString("sessionId", null) != null) }

    TextButton(
        onClick = {
            if (isLoggedIn) {
                // Logout logic
                prefs.edit { clear() }
                isLoggedIn = false
                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                // Optionally restart HomeActivity or refresh UI
                val intent = Intent(context, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // Navigate to LoginComposeActivity
                val intent = Intent(context, LoginComposeActivity::class.java)
                context.startActivity(intent)
            }
        },
        //modifier = Modifier.align(Alignment.CenterVertically)
    ) {
        Text(if (isLoggedIn) "Logout" else "Login")
    }
}


@Composable
fun BannerAdView() {
    LocalContext.current

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
                    adUnitId = "ca-app-pub-8250990399942328/4059345375" // ✅ Use Actual ID On Build
                   //adUnitId = "ca-app-pub-3940256099942544/6300978111" // ✅ Use TEST ID first Test Id

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



data class HomeButton(
    val text: String,
    val color: Color,
    val onClick: () -> Unit,
)