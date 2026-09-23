package users

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

class UserDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UserDetailsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    viewModel: UserDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val user = viewModel.userDetails.collectAsState().value
    val message = viewModel.message.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadUserDetails(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- PROFILE CIRCLE ----------
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.username?.firstOrNull()?.uppercase() ?: "",
                    fontSize = 40.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp)  // prevents overflow
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())  // ← SCROLL ADDED
                ) {

                    if (user == null) {
                        Text(text = message, color = Color.Red)
                    } else {
                        UserDetailItem("Username", user.username)
                        UserDetailItem("Name", user.messname)
                        UserDetailItem("Email", user.email)
                        UserDetailItem("Created At", user.created_at)
                        UserDetailItem("Verified", user.verified)

                        // Optional fields
                        UserDetailItem("Address", user.address)
                        UserDetailItem("GSTN", user.gstin)
                        UserDetailItem("PAN", user.pan)
                        UserDetailItem("Mobile", user.mobile)
                        UserDetailItem("Other Details", user.other_details)
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

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(text = safeValue, fontSize = 14.sp, color = Color.Gray)
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}
