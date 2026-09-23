package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.core.content.edit

class NameActivityAPI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppScreen1()
        }
    }
}

@SuppressLint("CommitPrefEdits", "ContextCastToActivity")
@Composable
fun AppScreen1() {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val sharedPrefs = remember { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }

    // Observe session
    var sessionToken by remember { mutableStateOf(sharedPrefs.getString("sessionId", null)) }

    // Redirect to login if no token
    LaunchedEffect(sessionToken) {
        if (sessionToken.isNullOrEmpty()) {
            val intent = Intent(context, LoginComposeActivity::class.java)
            intent.putExtra("redirectActivity", "NameActivityAPI") // ✅ pass back info
            context.startActivity(intent)
            activity?.finish()
        }
    }

    // If logged in, show main content
    if (!sessionToken.isNullOrEmpty()) {
        NameScreen1(sessionToken!!) {
            sharedPrefs.edit {
                remove("sessionId")
                remove("username")
            }
            sessionToken = null
            val intent = Intent(context, LoginComposeActivity::class.java)
            intent.putExtra("redirectActivity", "NameActivityAPI") // ✅ ensures correct redirect on re-login
            context.startActivity(intent)
            activity?.finish()
        }
    } else {
        // Loading UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFAAA1CD)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                "Redirecting to Login...",
                color = Color.White,
                modifier = Modifier.padding(top = 80.dp)
            )
        }
    }
}


@Composable
fun NameScreen1(sessionToken: String, onLogout: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue()) }
    var joinDate by remember { mutableStateOf(TextFieldValue()) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val namesList = remember { mutableStateListOf<NameRecord>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    var storedUsername by remember { mutableStateOf(sharedPrefs.getString("username", null)) }
    var selectedMember by remember { mutableStateOf<NameRecord?>(null) }
    var exitDate by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            joinDate = TextFieldValue("%02d-%02d-%d".format(d, m + 1, y))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Insert Name", style = MaterialTheme.typography.titleLarge)

            if (successMessage.isNotEmpty()) Text(
                successMessage,
                color = MaterialTheme.colorScheme.primary
            )
            if (errorMessage.isNotEmpty()) Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedTextField(
                value = name.text,
                onValueChange = { name = TextFieldValue(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = joinDate,
                onValueChange = {},
                label = { Text("Join Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        insertData1(
                            sessionToken, name.text, joinDate.text,
                            onSuccess = { successMessage = it; errorMessage = "" },
                            onError = { errorMessage = it; successMessage = "" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Submit") }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val username = storedUsername
                        if (!username.isNullOrEmpty()) {
                            retrieveNames2(
                                sessionToken,
                                username,
                                onSuccess = {
                                    namesList.clear()
                                    namesList.addAll(it)
                                },
                                onError = { errorMessage = it }
                            )
                        } else errorMessage = "Username not found."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Retrieve Names") }

            Button(onClick = { onLogout() }, modifier = Modifier.fillMaxWidth()) { Text("Logout") }

            Spacer(modifier = Modifier.height(16.dp))

            if (namesList.isNotEmpty()) {
                Text("Names Submitted by You:", style = MaterialTheme.typography.headlineSmall)
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray)
                            .padding(8.dp)
                    ) {
                        Text(
                            "Name",
                            Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            "Joined On",
                            Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            "Exited On",
                            Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .border(1.dp, Color.Black)
                    ) {
                        items(namesList) { item ->

                            val isActive =
                                item.exitDate.isNullOrBlank() || item.exitDate == "0000-00-00"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Black)
                                    .padding(8.dp)
                                    .background(
                                        if (selectedMember?.name == item.name) Color.LightGray
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = isActive) {
                                        selectedMember = item
                                    }
                            ) {
                                Text(item.name, Modifier.weight(1f))
                                Text(item.joinDate ?: "-", Modifier.weight(1f))
                                Text(item.exitDate ?: "Active", Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Exit Member", style = MaterialTheme.typography.titleMedium)

                    Text("Selected: ${selectedMember?.name ?: "None"}")

                    val context = LocalContext.current
                    val calendar = Calendar.getInstance()

                    Button(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    exitDate = "%02d-%02d-%04d".format(d, m + 1, y)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        enabled = selectedMember != null
                    ) {
                        Text(if (exitDate.isEmpty()) "Select Exit Date" else exitDate)
                    }
// 👇 ✅ PASTE YOUR SUBMIT BUTTON HERE (IMPORTANT)

                    val coroutineScope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            val member = selectedMember
                            val username = storedUsername ?: ""

                            if (member != null && exitDate.isNotEmpty()) {
                                Log.d("EXIT_DEBUG", "Name: ${member.name}")
                                coroutineScope.launch {

                                    // ✅ CALL suspend function properly
                                    exitMember(
                                        token = sessionToken,
                                        name = member.name,
                                        exitDate = exitDate,

                                        onSuccess = { msg ->
                                            successMessage = msg
                                            errorMessage = ""
                                            selectedMember = null
                                            exitDate = ""
                                        },

                                        onError = { err ->
                                            errorMessage = err
                                        }
                                    )

                                    // ✅ ALSO suspend → keep inside same coroutine
                                    if (username.isNotEmpty()) {
                                        retrieveNames2(
                                            sessionToken,
                                            username,
                                            onSuccess = { list ->
                                                namesList.clear()
                                                namesList.addAll(list)
                                            },
                                            onError = { errorMessage = it }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = selectedMember != null && exitDate.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Exit")
                    }
                }
            }
                // Month-wise Stats
                val monthlyStats = remember { mutableStateOf<Map<YearMonth, Int>>(emptyMap()) }
                var showStats by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val username = storedUsername ?: ""
                            if (username.isNotEmpty()) {
                                retrieveNames2(
                                    sessionToken,
                                    username,
                                    onSuccess = { records ->
                                        monthlyStats.value = calculateMonthlyActiveMembers1(records)
                                        showStats = true
                                    },
                                    onError = { Log.e("StatsError", it) }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Show Month-wise Active Stats") }

                if (showStats) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text(
                            "📊 Month-wise Active Members",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(8.dp)
                        ) {
                            Text(
                                "Month",
                                Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Active Members",
                                Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        monthlyStats.value.entries.forEachIndexed { index, (month, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.background
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "${
                                        month.month.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    } ${month.year}",
                                    Modifier.weight(1f)
                                )
                                Text(count.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }


// -----------------------------
// Retrofit API functions
// -----------------------------
suspend fun insertData1(
    token: String,
    name: String,
    joinDate: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val response = RetrofitClient.api.insertData(
            token = "Bearer $token",
            body = InsertRequest(name, joinDate, token)
        )

        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) onSuccess(body.message)
            else onError(body?.message ?: "Unknown error")
        } else onError("Server error: ${response.code()}")
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Network error")
    }
}

suspend fun retrieveNames2(
    sessionToken: String,
    username: String,
    onSuccess: (List<NameRecord>) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val response = RetrofitClient.api.getNames1(
            token = "Bearer $sessionToken",
            username = username
        )

        Log.d("API_CALL", "Response code: ${response.code()}")
        Log.d("API_CALL", "Response body: ${response.body()}")

        if (response.isSuccessful) {
            val body = response.body()
            Log.d("API_CALL", "Success: ${body?.success}")
            Log.d("API_CALL", "Message: ${body?.message}")
            Log.d("API_CALL", "Data: ${body?.data}")

            if (body?.success == true && body.data != null) {
                onSuccess(body.data)
            } else {
                onError(body?.message ?: "No data found")
            }
        } else onError("HTTP ${response.code()}")
    } catch (e: Exception) {
        Log.e("API_ERROR", "Exception: ${e.localizedMessage}", e)
        onError(e.localizedMessage ?: "Network error")
    }
}

fun calculateMonthlyActiveMembers1(records: List<NameRecord>): Map<YearMonth, Int> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Safely parse only valid records
    val parsedRecords = records.mapNotNull { record ->
        val joinDate = record.joinDate
            ?.takeIf { it.isNotBlank() && it != "0000-00-00" }
            ?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }
            ?: return@mapNotNull null // skip records with no valid join date

        val exitDate = record.exitDate
            ?.takeIf { it.isNotBlank() && it != "0000-00-00" }
            ?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }

        Triple(record.name, joinDate, exitDate)
    }

    if (parsedRecords.isEmpty()) return emptyMap()

    // Determine range of months to evaluate
    val startMonth = YearMonth.from(parsedRecords.minOf { it.second })
    val endMonth = YearMonth.from(parsedRecords.maxOf { it.third ?: LocalDate.now() })

    val result = mutableMapOf<YearMonth, Int>()
    var currentMonth = startMonth

    // Count active members month by month
    while (!currentMonth.isAfter(endMonth)) {
        val activeCount = parsedRecords.count { (_, join, exit) ->
            val exitMonth = exit?.let { YearMonth.from(it) }
            currentMonth >= YearMonth.from(join) &&
                    (exitMonth == null || currentMonth <= exitMonth)
        }
        result[currentMonth] = activeCount
        currentMonth = currentMonth.plusMonths(1)
    }

    return result
}


suspend fun exitMember(
    token: String,
    name: String,
    exitDate: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val response = RetrofitClient.api.exitMember(
            token = "Bearer $token",
            body = ExitRequest(name, exitDate, token)
        )

        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) {
                onSuccess(body.message)
            } else {
                onError(body?.message ?: "Unknown error")
            }
        } else {
            onError("Server error: ${response.code()}")
        }

    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Network error")
    }
}
