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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ------------------------------------------------------------
// Retrofit client & API definitions
// ------------------------------------------------------------
object RetrofitClientTotal {
    private const val BASE_URL = "https://www.legalcount.in/meal/"

    fun create(sessionToken: String?): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain: Interceptor.Chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $sessionToken")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

interface TotalApiService {
    @GET("total.php")
    fun getCombined(): Call<TotalApiResponse>
}

// DTOs for Retrofit (matches the JSON the server returns)
data class TotalApiResponse(
    val success: Boolean,
    val message: String?,
    val members: List<MemberDto>?,
    val transactions: List<TransactionDto>?
)

data class MemberDto(
    val name: String?,
    @SerializedName("join_dt") val join_dt: String?,
    @SerializedName("exit_dt") val exit_dt: String?
)

data class TransactionDto(
    @SerializedName("Name") val Name: String?,
    @SerializedName("Date") val Date: String?,
    @SerializedName("Item") val Item: String?,
    @SerializedName("Expenditure") val Expenditure: String?, // can come as string or number
    @SerializedName("Price") val Price: String?
)

// ------------------------------------------------------------
// Existing local models (kept for compatibility)
// ------------------------------------------------------------
data class NameRecord3(
    val name: String,
    val join_dt: String,
    val exit_dt: String,
)

data class TransactionRecord(
    val Name: String,
    val Date: String,
    val Item: String,
    val Expenditure: String,
    val Price: Double,
)

data class CombinedDataResult(
    val members: MutableList<NameRecord3>,
    val transactions: List<TransactionRecord>,
)

// ------------------------------------------------------------
// Activity + UI
// ------------------------------------------------------------
class TotalSummary : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppScreen2()
        }
    }
}


fun decodeBase64(encoded: String?): String {
    return try {
        if (encoded.isNullOrEmpty()) return ""
        val decodedBytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        String(decodedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        encoded ?: "" // fallback to raw value
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun AppScreen2() {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val sharedPrefs = remember { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }

    var sessionToken by remember {
        mutableStateOf(sharedPrefs.getString("sessionId", null))
    }

    // Redirect to Login if no token
    LaunchedEffect(Unit) {
        if (sessionToken.isNullOrEmpty()) {
            val intent = Intent(context, LoginComposeActivity::class.java)
            context.startActivity(intent)
            activity?.finish()
        }
    }

    // Show screen when token present
    sessionToken?.let { token ->
        NameScreen2(sessionToken = token, onLogout = {
            sharedPrefs.edit {
                remove("sessionId")
                remove("username")
            }
            sessionToken = null
            val intent = Intent(context, LoginComposeActivity::class.java)
            context.startActivity(intent)
            activity?.finish()
        })
    }
}

@Composable
fun NameScreen2(sessionToken: String, onLogout: () -> Unit) {
    var errorMessage by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<NameRecord3>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<TransactionRecord>>(emptyList()) }
    var summaryData by remember { mutableStateOf<List<MonthlyMemberBalance>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Button(
                onClick = { onLogout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }

        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        // call the retrofit-based function which only uses token
                        retrieveCombinedData(
                            sessionToken = sessionToken,
                            onSuccess = {
                                members = it.members
                                transactions = it.transactions
                                summaryData = processCombinedData(it.members, it.transactions)
                                errorMessage = ""
                            },
                            onError = {
                                errorMessage = it
                                summaryData = emptyList()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Summary Report")
            }
        }



        if (errorMessage.isNotEmpty()) {
            item {
                Text(text = errorMessage, color = Color.Red)
            }
        }

        if (summaryData.isNotEmpty()) {
            item {
                BalanceSummaryTable(summaryData = summaryData)
            }
        }
    }
}

// ------------------------------------------------------------
// Retrofit-based network call (replaces HttpURLConnection usage)
// ------------------------------------------------------------
suspend fun retrieveCombinedData(
    sessionToken: String?,
    onSuccess: (CombinedDataResult) -> Unit,
    onError: (String) -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            val retrofit = RetrofitClientTotal.create(sessionToken)
            val api = retrofit.create(TotalApiService::class.java)
            val response = api.getCombined().awaitResponse()

            if (!response.isSuccessful) {
                onError("HTTP ${response.code()} ${response.message()}")
                return@withContext
            }

            val body = response.body()
            if (body == null) {
                onError("Empty response body")
                return@withContext
            }

            if (!body.success) {
                onError(body.message ?: "Server returned success=false")
                return@withContext
            }

            // Map DTOs to local models (with safe parsing)
            val membersList = mutableListOf<NameRecord3>()
            body.members?.forEach { m ->
                membersList.add(
                    NameRecord3(
                        name = m.name.orEmpty(),
                        join_dt = m.join_dt.orEmpty(),
                        exit_dt = m.exit_dt.orEmpty()
                    )
                )
            }

            val transactionsList = mutableListOf<TransactionRecord>()
            body.transactions?.forEach { t ->
                val expenditure: String = t.Expenditure.orEmpty()
                val price = parseDoubleSafe(t.Price)
                transactionsList.add(
                    TransactionRecord(
                        Name = t.Name.orEmpty(),
                        Date = t.Date.orEmpty(),
                        Item = t.Item.orEmpty(),
                        Expenditure = expenditure,
                        Price = price
                    )
                )
            }

            onSuccess(CombinedDataResult(membersList, transactionsList))
        } catch (e: Exception) {
            Log.e("retrieveCombinedData", "Exception: ${e.message}", e)
            onError(e.localizedMessage ?: "Network error")
        }
    }
}

// small helper to parse numbers that may be sent as strings or numbers
private fun parseDoubleSafe(value: String?): Double {
    if (value == null) return 0.0
    return try {
        value.toDouble()
    } catch (_: Exception) {
        value.replace("[^0-9.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
    }
}

// ------------------------------------------------------------
// Processing logic (unchanged but slightly adapted to types)
// ------------------------------------------------------------
data class MonthlyMemberBalance(
    val month: String,
    val name: String,
    val opening: Double,
    val deposit: Double,
    val expenses: Double,
    val closing: Double,
)

fun processCombinedData(
    members: List<NameRecord3>,
    transactions: List<TransactionRecord>,
): List<MonthlyMemberBalance> {
    val balances = mutableListOf<MonthlyMemberBalance>()

    val allMonths = transactions.mapNotNull {
        parseDate1(it.Date)?.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }.distinct().sorted()

    val openingMap = mutableMapOf<String, Double>()

    for (month in allMonths) {
        val yearMonth = YearMonth.parse(month)

        val activeMembers = members.filter { member ->
            val joinDate = parseDate1(member.join_dt)?.let { YearMonth.from(it) }
            val exitDate = member.exit_dt.takeIf { it.isNotEmpty() }?.let { parseDate1(it)?.let { d -> YearMonth.from(d) } }

            joinDate != null && joinDate <= yearMonth && (exitDate == null || exitDate >= yearMonth)
        }

        val activeNames = activeMembers.map { it.name }

        val depositsByName = transactions.mapNotNull {
            val date = parseDate1(it.Date)
            if (date != null && YearMonth.from(date) == yearMonth && it.Item == "Deposit") it else null
        }.groupBy { it.Name }.mapValues { entry ->
            entry.value.sumOf { it.Price }
        }

        val totalExpenditure = transactions.mapNotNull {
            val date = parseDate1(it.Date)
            if (date != null && YearMonth.from(date) == yearMonth && it.Item != "Deposit") it else null
        }.sumOf { it.Price }

        Log.d("MonthExpenditure", "Month: $month -> Total Expenditure: $totalExpenditure")

        val sharePerMember = if (activeNames.isNotEmpty()) totalExpenditure / activeNames.size else 0.0

        for (name in activeNames) {
            val opening = openingMap[name] ?: 0.0
            val deposit = depositsByName[name] ?: 0.0
            val closing = opening + deposit - sharePerMember

            balances.add(
                MonthlyMemberBalance(
                    month = month,
                    name = name,
                    opening = opening,
                    deposit = deposit,
                    expenses = sharePerMember,
                    closing = closing
                )
            )
            openingMap[name] = closing
        }
    }

    return balances
}

// ------------------------------------------------------------
// Compose UI helpers (unchanged except for minor names)
// ------------------------------------------------------------
@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3F51B5))
    ) {
        repeat(6) { index ->
            TableCell(
                text = when (index) {
                    0 -> "Month"
                    1 -> "Name"
                    2 -> "Opening"
                    3 -> "Deposit"
                    4 -> "Expenditure"
                    else -> "Closing"
                },
                isHeader = true
            )
        }
    }
}

@Composable
fun BalanceRow(record: MonthlyMemberBalance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (record.name.hashCode() % 2 == 0) Color(0xFFF9F9F9) else Color.White)
    ) {
        TableCell(record.month)
        TableCell(record.name)
        TableCell("%.2f".format(record.opening))
        TableCell("%.2f".format(record.deposit))
        TableCell("%.2f".format(record.expenses))
        TableCell("%.2f".format(record.closing))
    }
}

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .border(0.5.dp, Color.LightGray)
            .padding(8.dp)
    ) {
        Text(
            text = text,
            textAlign = textAlign,
            fontSize = 14.sp,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) Color.White else Color.Black,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BalanceSummaryTable(summaryData: List<MonthlyMemberBalance>) {

    var filter by remember { mutableStateOf(BalanceFilter()) }

    val currentYearMonth =
        remember { YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) }

    val years = remember(summaryData) {
        summaryData.map { it.month.substring(0, 4) }.distinct().sortedDescending()
    }

    val months = remember {
        listOf("01","02","03","04","05","06","07","08","09","10","11","12")
    }

    val names = remember(summaryData) {
        summaryData.map { it.name }.distinct()
    }

    // 🔥 CORE LOGIC
    val filteredData = summaryData.filter { record ->

        val isFilterEmpty =
            filter.year.isBlank() &&
                    filter.month.isBlank() &&
                    filter.name.isBlank()

        // DEFAULT → CURRENT MONTH ONLY
        if (isFilterEmpty) {
            record.month == currentYearMonth
        } else {
            val yearMatch =
                filter.year.isBlank() || record.month.startsWith(filter.year)

            val monthMatch =
                filter.month.isBlank() || record.month.substring(5, 7) == filter.month

            val nameMatch =
                filter.name.isBlank() || record.name == filter.name

            yearMatch && monthMatch && nameMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        Text(
            text = "Monthly Member Balance Summary",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 8.dp)
                .border(1.dp, Color.Gray)
        ) {
            Column {

                // FILTER ROW
                FilterDropdownRow(
                    years = years,
                    months = months,
                    names = names,
                    filter = filter,
                    onFilterChange = { filter = it }
                )

                TableHeader()

                filteredData.forEach { record ->
                    BalanceRow(record)
                }
            }
        }
    }
    Button(onClick = { filter = BalanceFilter() }) {
        Text("Current Month")
    }
}

@Composable
fun FilterDropdownRow(
    years: List<String>,
    months: List<String>,
    names: List<String>,
    filter: BalanceFilter,
    onFilterChange: (BalanceFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
    ) {

        // YEAR DROPDOWN
        DropdownFilter(
            label = "Year",
            options = listOf("") + years,
            selectedOption = filter.year,
            onOptionSelected = {
                onFilterChange(filter.copy(year = it))
            }
        )

        // MONTH DROPDOWN
        DropdownFilter(
            label = "Month",
            options = listOf("") + months,
            selectedOption = filter.month,
            onOptionSelected = {
                onFilterChange(filter.copy(month = it))
            }
        )

        // NAME DROPDOWN
        DropdownFilter(
            label = "Name",
            options = listOf("") + names,
            selectedOption = filter.name,
            onOptionSelected = {
                onFilterChange(filter.copy(name = it))
            }
        )

        repeat(3) { TableCell(text = "") }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .width(120.dp)
            .border(0.5.dp, Color.LightGray)
            .padding(4.dp)
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(label, fontSize = 12.sp) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(56.dp),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.ifEmpty { "All" }, fontSize = 12.sp) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class BalanceFilter(
    val year: String = "",
    var month: String = "",
    var name: String = "",
)

// ------------------------------------------------------------
// Date parsing helper (unchanged)
// ------------------------------------------------------------
fun parseDate1(dateStr: String): LocalDate? {
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
            // Ignore and try next format
        }
    }
    return null
}
