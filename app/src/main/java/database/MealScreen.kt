package database

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kushal.mealapp.BannerAdView
import com.kushal.mealapp.GlobalCalculatorOverlay
import com.kushal.mealapp.GoogleSignInActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(viewModel: MealViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())
    val allMeals by viewModel.allMeals1.observeAsState(emptyList())

    var showProLimitDialog by remember { mutableStateOf(false) }
    val sharedPrefs = remember(context) { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }
    val isProUser = sharedPrefs.getBoolean("isProVersion", false)

    fun onAddMemberClicked() {
        if (!isProUser && allMembers.size >= 10) {
            showProLimitDialog = true
        } else {
            navController.navigate("member_form")
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF8F9FA),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(300.dp)
            ) {
                // Header Banner
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Journal DayBook",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isProUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFD700)
                                    ) {
                                        Text(
                                            "🌟 PRO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3E2723),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "Offline Expense Manager",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "EXPENSE MENU",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                // 1. Add Member / Account
                NavigationDrawerItem(
                    label = { Text("Add Member / Account", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.GroupAdd, contentDescription = null, tint = Color(0xFF1976D2)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAddMemberClicked()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 2. Member / Entity Table
                NavigationDrawerItem(
                    label = { Text("Member / Account", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF00897B)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (allMembers.isNotEmpty()) {
                            navController.navigate("entity")
                        } else {
                            Toast.makeText(context, "No members or personal accounts available!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 3. Add Expenditure
                NavigationDrawerItem(
                    label = { Text("Add Expenditure", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFFF57C00)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("meal_form")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 4. Summary Table
                NavigationDrawerItem(
                    label = { Text("Summary", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Color(0xFF7B1FA2)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (allMeals.isNotEmpty()) {
                            navController.navigate("summary_table1")
                        } else {
                            Toast.makeText(context, "No data available!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 5. Member / Account Report
                NavigationDrawerItem(
                    label = { Text("Report", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Assessment, contentDescription = null, tint = Color(0xFF388E3C)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("member_report")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 6. Pie Chart
                NavigationDrawerItem(
                    label = { Text("Expense Pie Chart", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.InsertChart, contentDescription = null, tint = Color(0xFFD81B60)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("chart")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 7. Listwise
                NavigationDrawerItem(
                    label = { Text("Monthly Listwise", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.InsertChart, contentDescription = null, tint = Color(0xFF0288D1)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("listwise")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // 8. Google Drive Sync
                NavigationDrawerItem(
                    label = { Text("Backup to Drive", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF1565C0)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        context.startActivity(Intent(context, GoogleSignInActivity::class.java))
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer Mode Chip
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFE0F2F1), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ Mode: Offline Expense App",
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
                .systemBarsPadding(),
            containerColor = Color(0xFFF4F6F9),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Expenses Dashboard",
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
                                contentDescription = "Open Menu",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        if (isProUser) {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        Toast.makeText(context, "🌟 PRO MEMBER Active: Unlimited Members & Features Unlocked", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFD700)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌟 ", fontSize = 13.sp)
                                    Text(
                                        "PRO MEMBER",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF3E2723)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        val intent = Intent(context, com.kushal.mealapp.ProBillingActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1565C0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("👑 ", fontSize = 13.sp)
                                    Text(
                                        "PRO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
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
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp)
            ) {
                // Dashboard Grid Menu
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ProfessionalMenuItem(
                            title = "Add Member / Account",
                            icon = Icons.Outlined.GroupAdd,
                            badgeColor = Color(0xFF1976D2),
                            bgColor = Color(0xFFE3F2FD),
                            onClick = { onAddMemberClicked() }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Member / Account",
                            icon = Icons.Outlined.Person,
                            badgeColor = Color(0xFF00897B),
                            bgColor = Color(0xFFE0F2F1),
                            onClick = {
                                if (allMembers.isNotEmpty()) {
                                    navController.navigate("entity")
                                } else {
                                    Toast.makeText(context, "No members or personal accounts available!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Add Expenditure",
                            icon = Icons.Outlined.Edit,
                            badgeColor = Color(0xFFF57C00),
                            bgColor = Color(0xFFFFF3E0),
                            onClick = { navController.navigate("meal_form") }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Summary Table",
                            icon = Icons.Outlined.BarChart,
                            badgeColor = Color(0xFF7B1FA2),
                            bgColor = Color(0xFFF3E5F5),
                            onClick = {
                                if (allMeals.isNotEmpty()) {
                                    navController.navigate("summary_table1")
                                } else {
                                    Toast.makeText(context, "No data available!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Member / Account Report",
                            icon = Icons.Outlined.Assessment,
                            badgeColor = Color(0xFF388E3C),
                            bgColor = Color(0xFFE8F5E9),
                            onClick = { navController.navigate("member_report") }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Expense Pie Chart",
                            icon = Icons.Outlined.InsertChart,
                            badgeColor = Color(0xFFD81B60),
                            bgColor = Color(0xFFFCE4EC),
                            onClick = { navController.navigate("chart") }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Monthly Listwise",
                            icon = Icons.Outlined.InsertChart,
                            badgeColor = Color(0xFF0288D1),
                            bgColor = Color(0xFFE1F5FE),
                            onClick = { navController.navigate("listwise") }
                        )
                    }
                    item {
                        ProfessionalMenuItem(
                            title = "Loan Account & Reminders",
                            icon = Icons.Outlined.Payments,
                            badgeColor = Color(0xFF8E24AA),
                            bgColor = Color(0xFFF3E5F5),
                            onClick = {
                                val intent = Intent(context, com.kushal.mealapp.LoanActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Disclaimer Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Disclaimer: Offline data is stored on this device. Uninstalling the app will delete data unless backed up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        }

        // PRO Version Limit Dialog (Free Limit: 10 Members / Accounts)
        if (showProLimitDialog) {
            AlertDialog(
                onDismissRequest = { showProLimitDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑 ", fontSize = 22.sp)
                        Text(
                            text = "PRO Version Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF002B49)
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Free version limit of 10 members / accounts reached (${allMembers.size}/10 created).",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Upgrade to the PRO Version to add unlimited group members and personal accounts!",
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showProLimitDialog = false
                            val intent = Intent(context, com.kushal.mealapp.ProBillingActivity::class.java)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upgrade to PRO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProLimitDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        GlobalCalculatorOverlay()
    }
}

/**
 * Professional Dashboard Card Component with Colored Badge
 */
@Composable
fun ProfessionalMenuItem(
    title: String,
    icon: ImageVector,
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
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = badgeColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )
        }
    }
}