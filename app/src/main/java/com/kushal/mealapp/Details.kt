package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Details : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DetailScreen()
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun DetailScreen() {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val sharedPrefs = remember { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }

    var sessionToken by remember { mutableStateOf(sharedPrefs.getString("sessionId", null) ?: sharedPrefs.getString("sessionId1", null)) }

    // Redirect to LoginComposeActivity if not logged in
    LaunchedEffect(Unit) {
        if (sessionToken.isNullOrEmpty()) {
            sharedPrefs.edit {
                putString("redirectActivity", "Details")
            }

            val intent = Intent(context, LoginComposeActivity::class.java)
            context.startActivity(intent)
            activity?.finish()
        }
    }

    sessionToken?.let { token ->
        DetailScreen1(token) {
            // On Logout
            sharedPrefs.edit {
                remove("sessionId")
                remove("username")
                remove("redirectActivity")
            }
            sessionToken = null

            val intent = Intent(context, LoginComposeActivity::class.java)
            context.startActivity(intent)
            activity?.finish()
        }
    }
}

// ------------------ Data Models ------------------
data class NameEntry(
    val name: String,
    val joinDate: String,
    val amount: String,
    val item: String,
    val expenditure: String,
)

// ------------------ UI Logic ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen1(sessionToken: String, onLogout: () -> Unit) {
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var namesList by remember { mutableStateOf<List<NameEntry>>(emptyList()) }
    var selectedName by remember { mutableStateOf("All") }
    var selectedItem by remember { mutableStateOf("All") }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentDate = LocalDate.now()
    var members by remember { mutableStateOf<List<NameRecord3>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<TransactionRecord>>(emptyList()) }
    var summaryData by remember { mutableStateOf<List<MonthlyMemberBalance>>(emptyList()) }

    var showStats0 by remember { mutableStateOf(false) }
    var showStats7 by remember { mutableStateOf(false) }

    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val years = (2024..2026).map { it.toString() }

    val nameOptions = remember(namesList) {
        listOf("All") + namesList.map { it.name }.distinct().sorted()
    }
    val itemOptions = remember(namesList) {
        listOf("All") + namesList
            .mapNotNull { it.item?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    var selectedMonth1 by remember { mutableStateOf(months[currentDate.monthValue - 1]) }
    var selectedYear1 by remember { mutableStateOf(currentDate.year.toString()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val storedUsername = remember { getUsernameFromSharedPrefs1(context) }

    val activity = remember(context) { context as? Activity }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Expenditures & Reports",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF002B49)
                        )
                        if (storedUsername != null) {
                            Text(
                                "Logged in as: $storedUsername",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success or Error Message Card
            if (successMessage.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else if (errorMessage.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Navigation / Action Buttons Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Reports & Quick Actions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )

                    // 1. Day Wise Expenditures Details
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (showStats0) {
                                    showStats0 = false
                                } else {
                                    if (storedUsername != null) {
                                        retrieveNames1(
                                            sessionToken,
                                            username = storedUsername,
                                            onSuccess = {
                                                namesList = it
                                                showStats0 = true
                                            },
                                            onError = { errorMessage = it }
                                        )
                                    } else {
                                        errorMessage = "Username not found in session."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showStats0) Color(0xFF0288D1) else Color(0xFF1565C0)
                        )
                    ) {
                        Text(
                            if (showStats0) "Hide Day Wise Expenditures" else "📅 Day Wise Expenditures Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Summary Report
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (showStats7) {
                                    showStats7 = false
                                } else {
                                    if (storedUsername != null) {
                                        retrieveCombinedData(
                                            sessionToken = sessionToken,
                                            onSuccess = {
                                                members = it.members
                                                transactions = it.transactions
                                                summaryData = processCombinedData(it.members, it.transactions)

                                                showStats7 = true
                                                showStats0 = false
                                                errorMessage = ""
                                            },
                                            onError = { error ->
                                                errorMessage = error
                                                summaryData = emptyList()
                                                members = emptyList()
                                                transactions = emptyList()
                                                showStats7 = false
                                            }
                                        )
                                    } else {
                                        errorMessage = "Username not found in session."
                                        summaryData = emptyList()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showStats7) Color(0xFF2E7D32) else Color(0xFF388E3C)
                        )
                    ) {
                        Text(
                            if (showStats7) "Hide Summary Report" else "📊 Summary Report",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Open Dashboard
                        Button(
                            onClick = {
                                val intent = Intent(context, DashboardActivity::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                        ) {
                            Text("⚡ Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Back Button
                        OutlinedButton(
                            onClick = {
                                activity?.finish()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("◀ Back", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // -------- Display Expenditure Table ----------
            if (showStats0 && namesList.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Filter Expenditures",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF002B49),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Scrollable Filters Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month Filter
                            var expandedMonth1 by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedMonth1 = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Month: $selectedMonth1", fontSize = 12.sp, color = Color(0xFF1565C0))
                                }
                                DropdownMenu(
                                    expanded = expandedMonth1,
                                    onDismissRequest = { expandedMonth1 = false }
                                ) {
                                    months.forEach { month ->
                                        DropdownMenuItem(
                                            text = { Text(month) },
                                            onClick = {
                                                selectedMonth1 = month
                                                selectedDate = null
                                                expandedMonth1 = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Date Filter Button
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    selectedDate?.toString() ?: "Filter Date",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1565C0)
                                )
                            }

                            // Name Filter
                            var expandedName by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedName = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Name: $selectedName", fontSize = 12.sp, color = Color(0xFF1565C0))
                                }
                                DropdownMenu(
                                    expanded = expandedName,
                                    onDismissRequest = { expandedName = false }
                                ) {
                                    nameOptions.forEach { name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedName = name
                                                expandedName = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Item Filter
                            var expandedItem by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedItem = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Item: $selectedItem", fontSize = 12.sp, color = Color(0xFF1565C0))
                                }
                                DropdownMenu(
                                    expanded = expandedItem,
                                    onDismissRequest = { expandedItem = false }
                                ) {
                                    itemOptions.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                selectedItem = item
                                                expandedItem = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Year Filter
                            var expandedYear1 by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedYear1 = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Year: $selectedYear1", fontSize = 12.sp, color = Color(0xFF1565C0))
                                }
                                DropdownMenu(
                                    expanded = expandedYear1,
                                    onDismissRequest = { expandedYear1 = false }
                                ) {
                                    years.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text(year) },
                                            onClick = {
                                                selectedYear1 = year
                                                selectedDate = null
                                                expandedYear1 = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            datePickerState.selectedDateMillis?.let { millis ->
                                                selectedDate = Instant.ofEpochMilli(millis)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDate()
                                            }
                                            showDatePicker = false
                                        }
                                    ) { Text("OK") }
                                },
                                dismissButton = {
                                    Button(onClick = { showDatePicker = false }) { Text("Cancel") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val monthIndex1 = months.indexOf(selectedMonth1) + 1
                        val selectedYearInt1 = selectedYear1.toInt()

                        val displayList = namesList
                            .filter {
                                parseDate(it.joinDate)?.let { parsed ->
                                    val dateMatch = if (selectedDate != null) {
                                        parsed == selectedDate
                                    } else {
                                        parsed.monthValue == monthIndex1 && parsed.year == selectedYearInt1
                                    }

                                    dateMatch &&
                                            (selectedName == "All" || it.name == selectedName) &&
                                            when {
                                                selectedItem.equals("All", true) -> !it.item.equals("Deposit", true)
                                                selectedItem.equals("Deposit", true) -> it.item.equals("Deposit", true)
                                                else -> it.item.equals(selectedItem, true)
                                            }
                                } == true
                            }
                            .sortedByDescending { parseDate(it.joinDate) }

                        val totalAmount = displayList.sumOf {
                            it.amount.toDoubleOrNull() ?: 0.0
                        }

                        // Styled Table Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        ) {
                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE0F7FA))
                                    .padding(vertical = 10.dp, horizontal = 10.dp)
                            ) {
                                Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF002B49), fontSize = 13.sp)
                                Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF002B49), fontSize = 13.sp)
                                Text("Item", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF002B49), fontSize = 13.sp)
                                Text("Amount", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF002B49), fontSize = 13.sp, textAlign = TextAlign.End)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            var expandedIndex by remember { mutableStateOf(-1) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (displayList.isEmpty()) {
                                    Text(
                                        "No records found for current filters.",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                displayList.forEachIndexed { index, item ->
                                    val rowBg = if (index % 2 == 0) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(rowBg)
                                                .clickable { expandedIndex = if (expandedIndex == index) -1 else index }
                                                .padding(vertical = 10.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.joinDate, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.DarkGray)
                                            Text(item.name, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
                                            Text(item.item, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFF6A1B9A))

                                            Text(
                                                text = "₹${item.amount}",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF2E7D32),
                                                textAlign = TextAlign.End
                                            )
                                        }

                                        if (expandedIndex == index) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Details:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1565C0))
                                                    Text("Name: ${item.name}", fontSize = 12.sp)
                                                    Text("Item: ${item.item}", fontSize = 12.sp)
                                                    Text("Amount: ₹${item.amount}", fontSize = 12.sp)
                                                    Text("Date: ${item.joinDate}", fontSize = 12.sp)
                                                    Text("Remark: ${item.expenditure}", fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        if (index < displayList.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }

                            // Total Row Footer
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Expenditures:",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "₹${"%.2f".format(totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // -------- Summary Report --------
            if (showStats7 && summaryData.isNotEmpty()) {
                BalanceSummaryTable(summaryData = summaryData)
            }
        }
    }
}

// ------------------ Helper Functions ------------------
fun getUsernameFromSharedPrefs1(context: Context): String? {
    val sharedPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    return sharedPrefs.getString("username", null)
}

suspend fun retrieveNames1(
    sessionToken: String,
    username: String,
    onSuccess: (List<NameEntry>) -> Unit,
    onError: (String) -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.legalcount.in/meal/getdetailsapp.php?username=$username")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $sessionToken")

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                onError("Server error: $responseCode")
                return@withContext
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            if (!json.getBoolean("success")) {
                onError(json.getString("message"))
                return@withContext
            }

            val namesList = mutableListOf<NameEntry>()
            val dataArray = json.getJSONArray("data")

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val name = obj.getString("Name")
                val joinDate = obj.getString("Date")
                val amount = obj.getString("Price")
                val item = obj.getString("Item")
                val expenditure = obj.getString("Expenditure")
                namesList.add(NameEntry(name, joinDate, amount, item, expenditure))
            }

            onSuccess(namesList)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Unknown error")
        }
    }
}

fun parseDate(dateStr: String): LocalDate? {
    val formatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    )

    for (formatter in formatters) {
        try {
            return LocalDate.parse(dateStr, formatter)
        } catch (_: Exception) {
        }
    }
    return null
}
