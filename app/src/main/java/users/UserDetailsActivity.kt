package users

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kushal.mealapp.BannerAdView

class UserDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UserDetailsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    onBack: () -> Unit = {},
    viewModel: UserDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by viewModel.userDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserDetails(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BannerAdView()
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            // ---------- PROFILE CIRCLE ----------
            val initial = user?.username?.firstOrNull()?.uppercase()
                ?: user?.messname?.firstOrNull()?.uppercase()
                ?: "U"

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 38.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (user?.messname != null) {
                Text(
                    text = user!!.messname!!,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF002B49)
                )
            }

            val sharedPrefs = remember { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }
            val isPro = sharedPrefs.getBoolean("isProVersion", false)

            if (isPro) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFD700)
                ) {
                    Text(
                        text = "🌟 PRO MEMBER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Loading user details...", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else if (user == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (message.isNotBlank()) message else "No user profile data available.",
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        UserDetailItem("Username", user?.username)
                        UserDetailItem("Mess / Entity Name", user?.messname)
                        UserDetailItem("Email Address", user?.email)
                        UserDetailItem("Account Created", user?.created_at)
                        UserDetailItem("Verification Status", user?.verified)

                        UserDetailItem("Address", user?.address)
                        UserDetailItem("GSTIN", user?.gstin)
                        UserDetailItem("PAN Number", user?.pan)
                        UserDetailItem("Mobile", user?.mobile)
                        UserDetailItem("Other Details", user?.other_details)
                    }
                }
            }
        }
    }
}

// ---------- HELPER ----------
@Composable
fun UserDetailItem(label: String, value: String?) {
    val safeValue = when {
        value == null -> "-"
        value.trim().isEmpty() -> "-"
        value.equals("null", ignoreCase = true) -> "-"
        else -> value
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1565C0))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = safeValue, fontSize = 14.sp, color = Color(0xFF374151))
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
    }
}
