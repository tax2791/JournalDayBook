package database

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.BannerAdView
import com.kushal.mealapp.DriveBackupManager
import com.kushal.mealapp.GlobalCalculatorOverlay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun MemberForm(
    viewModel: MealViewModel,
    onFormSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())
    var showProDialog by remember { mutableStateOf(false) }

    // Form mode: "Group Member" or "Personal Account"
    var entryType by remember { mutableStateOf("Group Member") }
    val entryTypes = listOf("Group Member", "Personal Account")

    // Common fields
    var name by remember { mutableStateOf("") }
    var openingBalance by remember { mutableStateOf("") }
    var joinDate by remember { mutableStateOf<Date?>(Date()) }

    // Personal Account specific fields
    var accountType by remember { mutableStateOf("Savings") }
    var expandedAccountType by remember { mutableStateOf(false) }
    val accountTypes = listOf("Savings", "Deposit", "Credit Card", "Cash Wallet", "Current","Investment", "Other")

    // Group Member specific fields
    var exitDate by remember { mutableStateOf<Date?>(null) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val scrollState = rememberScrollState()

    fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val accentColor = if (entryType == "Group Member") Color(0xFF1976D2) else Color(0xFF7B1FA2)
    val headerBgColor = if (entryType == "Group Member") Color(0xFFE3F2FD) else Color(0xFFF3E5F5)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = Color(0xFFF4F6F9),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Member & Account Registration",
                            color = Color(0xFF002B49),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Add Group Members or Personal Accounts",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode Selector Pill Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    entryTypes.forEach { type ->
                        val isSelected = entryType == type
                        val modeColor = if (type == "Group Member") Color(0xFF1976D2) else Color(0xFF7B1FA2)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) modeColor else Color.Transparent)
                                .clickable { entryType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (type == "Group Member") "👥 Group Member" else "💳 Personal Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else Color(0xFF555555),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Main Form Container Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Accent Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBgColor)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (entryType == "Group Member") "👥" else "💳",
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (entryType == "Group Member") "Add Group Member" else "Add Personal Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Form Fields Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Name Input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (entryType == "Group Member") "Member Name" else "Account Label / Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 2. Personal Account Sub-Type Dropdown
                        if (entryType == "Personal Account") {
                            ExposedDropdownMenuBox(
                                expanded = expandedAccountType,
                                onExpandedChange = { expandedAccountType = !expandedAccountType },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = accountType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Account Sub-Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountType) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedAccountType,
                                    onDismissRequest = { expandedAccountType = false }
                                ) {
                                    accountTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                accountType = type
                                                expandedAccountType = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Opening Balance (₹)
                        OutlinedTextField(
                            value = openingBalance,
                            onValueChange = { openingBalance = it },
                            label = { Text("Opening Balance (₹)") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 4. Opening / Join Date Field
                        OutlinedTextField(
                            value = joinDate?.let { dateFormat.format(it) } ?: "Select Opening Balance Date",
                            onValueChange = {},
                            label = { Text(if (entryType == "Group Member") "Join Date / Opening Date" else "Opening Balance Date") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Select Date",
                                    modifier = Modifier.clickable { showDatePicker { joinDate = it } }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 5. Exit Date (Group Members only)
                        if (entryType == "Group Member") {
                            OutlinedTextField(
                                value = exitDate?.let { dateFormat.format(it) } ?: "Select Exit Date (Optional)",
                                onValueChange = {},
                                label = { Text("Exit Date (Optional)") },
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select Exit Date",
                                        modifier = Modifier.clickable { showDatePicker { exitDate = it } }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        val isFormValid = name.isNotBlank() && joinDate != null

                        Button(
                            onClick = {
                                if (allMembers.size >= 20) {
                                    showProDialog = true
                                    return@Button
                                }

                                val balanceValue = openingBalance.toDoubleOrNull() ?: 0.0

                                if (entryType == "Group Member") {
                                    viewModel.addGroupMember(
                                        name = name.trim(),
                                        openingBalance = balanceValue,
                                        joinDate = joinDate!!,
                                        exitDate = exitDate
                                    )
                                } else {
                                    viewModel.addPersonalAccount(
                                        accountName = name.trim(),
                                        accountType = accountType,
                                        openingBalance = balanceValue,
                                        joinDate = joinDate!!,
                                        exitDate = exitDate
                                    )
                                }

                                Toast.makeText(
                                    context,
                                    if (entryType == "Group Member") "Group member added successfully ✅" else "Personal account added successfully ✅",
                                    Toast.LENGTH_SHORT
                                ).show()

                                name = ""
                                openingBalance = ""
                                joinDate = Date()
                                exitDate = null

                                DriveBackupManager.syncAutomatically(context)
                                onFormSubmitted()
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(
                                text = if (entryType == "Group Member") "Save Group Member" else "Save Personal Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // PRO Limit Dialog
        if (showProDialog) {
            AlertDialog(
                onDismissRequest = { showProDialog = false },
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
                            text = "Free version is limited to 20 members / accounts (${allMembers.size}/20 created).",
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
                            showProDialog = false
                            Toast.makeText(context, "Contact support for PRO Version upgrade", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upgrade to PRO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        GlobalCalculatorOverlay()
    }
}
