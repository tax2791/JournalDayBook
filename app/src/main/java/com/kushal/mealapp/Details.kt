package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
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

    var sessionToken by remember { mutableStateOf(sharedPrefs.getString("sessionId", null)) }

    // 🔁 Redirect to LoginComposeActivity if not logged in
    LaunchedEffect(Unit) {
        if (sessionToken.isNullOrEmpty()) {
            // Save the redirect target (this activity)
            sharedPrefs.edit {
                putString("redirectActivity", "Details")
            }

            val intent = Intent(context, LoginComposeActivity::class.java)
            context.startActivity(intent)
            activity?.finish()
        }
    }

    // ✅ If sessionToken exists, show the detail screen
    sessionToken?.let { token ->
        DetailScreen1(token) {
            // On Logout
            sharedPrefs.edit {
                remove("sessionId")
                remove("username")
                remove("redirectActivity")
            }
            sessionToken = null

            // Redirect to LoginComposeActivity
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

    LaunchedEffect(transactions) {
        Log.d("ITEM_DEBUG", "Items: ${transactions.map { it.Item }}")
    }



    var selectedMonth1 by remember { mutableStateOf(months[currentDate.monthValue - 1]) }
    var selectedYear1 by remember { mutableStateOf(currentDate.year.toString()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "My Expenditures", style = MaterialTheme.typography.titleLarge)

            // success/error messages
            if (successMessage.isNotEmpty()) {
                Text(text = successMessage, color = MaterialTheme.colorScheme.primary)
            } else if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }

            val storedUsername = remember { getUsernameFromSharedPrefs1(context) }

            Button(onClick = { onLogout() }, modifier = Modifier.fillMaxWidth()) {
                Text("Logout")
            }

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Day Wise Expenditures Details")
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                        showStats0 = false   // 👈 prevent UI overlap
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Summary Report")
            }

            Button(
                onClick = {
                    val intent = Intent(context, DashboardActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Dashboard")
            }


            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = {
                context.startActivity(Intent(context, HomeActivity::class.java))
            }) { Text("Back Home") }

            // -------- Display Expenditure Table ----------
            if (showStats0) {
                if (namesList.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        var expandedMonth1 by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { expandedMonth1 = true }) {
                                Text(selectedMonth1)
                            }
                            DropdownMenu(
                                expanded = expandedMonth1,
                                onDismissRequest = { expandedMonth1 = false }) {
                                months.forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(month) },
                                        onClick = {
                                            selectedMonth1 = month
                                            selectedDate = null      // Clear date filter
                                            expandedMonth1 = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showDatePicker = true }
                        ) {
                            Text(
                                selectedDate?.toString() ?: "Date"
                            )
                        }

                        if (showDatePicker) {

                            val datePickerState = rememberDatePickerState()

                            DatePickerDialog(
                                onDismissRequest = {
                                    showDatePicker = false
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            datePickerState.selectedDateMillis?.let { millis ->

                                                selectedDate =
                                                    java.time.Instant.ofEpochMilli(millis)
                                                        .atZone(java.time.ZoneId.systemDefault())
                                                        .toLocalDate()
                                                expandedMonth1 = false
                                            }

                                            showDatePicker = false
                                        }
                                    ) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = {
                                            showDatePicker = false
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                DatePicker(
                                    state = datePickerState
                                )
                            }
                        }
                        /* ---------- Name Dropdown (NEW) ---------- */
                        var expandedName by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { expandedName = true }) {
                                Text(selectedName)
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
                        /* ---------- Item Dropdown (NEW) ---------- */
                        var expandedItem by remember { mutableStateOf(false) }

                        Box {
                            Button(onClick = { expandedItem = true }) {
                                Text(selectedItem)
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

                        /* ---------- Year Dropdown (NEW) ---------- */

                        var expandedYear1 by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { expandedYear1 = true }) {
                                Text(selectedYear1)
                            }
                            DropdownMenu(
                                expanded = expandedYear1,
                                onDismissRequest = { expandedYear1 = false }) {
                                years.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year) },
                                        onClick = {
                                            selectedYear1 = year
                                            selectedDate = null      // Clear date filter
                                            expandedYear1 = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.LightGray)
                                .padding(8.dp)
                        ) {
                            Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("Item", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("Amount", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.Red)
                        }

                        var expandedIndex by remember { mutableStateOf(-1) }
                        val monthIndex1 = months.indexOf(selectedMonth1) + 1
                        val selectedYearInt1 = selectedYear1.toInt()

                        val displayList = namesList
                            .filter {
                                parseDate(it.joinDate)?.let { parsed ->

                                    val dateMatch =
                                        if (selectedDate != null) {
                                            parsed == selectedDate
                                        } else {
                                            parsed.monthValue == monthIndex1 &&
                                                    parsed.year == selectedYearInt1
                                        }

                                    dateMatch &&
                                            (selectedName == "All" || it.name == selectedName) &&
                                            when {
                                                selectedItem.equals("All", true) ->
                                                    !it.item.equals("Deposit", true)

                                                selectedItem.equals("Deposit", true) ->
                                                    it.item.equals("Deposit", true)

                                                else ->
                                                    it.item.equals(selectedItem, true)
                                            }
                                } == true
                            }

                            .sortedByDescending { parseDate(it.joinDate) }
                        val totalAmount = displayList.sumOf {
                            it.amount.toDoubleOrNull() ?: 0.0
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // =========================
                            // ✅ SCROLLABLE DATA ONLY
                            // =========================
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {

                                displayList.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.Black)
                                            .padding(8.dp)
                                    ) {
                                        Text(item.joinDate, modifier = Modifier.weight(1f))
                                        Text(item.name, modifier = Modifier.weight(1f))
                                        Text(item.item, modifier = Modifier.weight(1f))

                                        Box(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.amount,
                                                modifier = Modifier.clickable {
                                                    expandedIndex = if (expandedIndex == index) -1 else index
                                                },
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            DropdownMenu(
                                                expanded = expandedIndex == index,
                                                onDismissRequest = { expandedIndex = -1 }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "Name: ${item.name}\nAmount: ₹${item.amount}\nDate: ${item.joinDate}\nItem: ${item.item}\nExpenditure: ${item.expenditure}"
                                                        )
                                                    },
                                                    onClick = { expandedIndex = -1 }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // =========================
                            // ✅ TOTAL ROW (FIXED)
                            // =========================
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, Color.Black)
                                    .background(Color(0xFFE0E0E0))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "Total (Rs.)",
                                    modifier = Modifier.weight(3f),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Text(
                                    text = "₹${"%.2f".format(totalAmount)}",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }
                                }
                            }
                        }




            // -------- Summary Report --------
            Spacer(modifier = Modifier.height(16.dp))
            if (showStats7 && summaryData.isNotEmpty()) {
                BalanceSummaryTable(summaryData = summaryData)

                // go to TotalSummary.kt code to get the BalanceSummaryTable
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
