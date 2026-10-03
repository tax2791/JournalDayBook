package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
    @SerializedName("Expenditure") val Expenditure: String?,
    @SerializedName("Price") val Price: String?
)

// ------------------------------------------------------------
// Existing local models
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
        val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
        String(decodedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        encoded ?: ""
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameScreen2(sessionToken: String, onLogout: () -> Unit) {
    var errorMessage by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<NameRecord3>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<TransactionRecord>>(emptyList()) }
    var summaryData by remember { mutableStateOf<List<MonthlyMemberBalance>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Auto-fetch data on screen launch
    LaunchedEffect(sessionToken) {
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Total Balance Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF002B49)
                        )
                        Text(
                            "Monthly Member Balances & Deposits",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onLogout() }) {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Action Card
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
                        "Report Actions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
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
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("🔄 Refresh Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(context, HomeActivity::class.java))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🏠 Back Home", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
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

            if (summaryData.isNotEmpty()) {
                BalanceSummaryTable(summaryData = summaryData)
            } else if (errorMessage.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading summary data...", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Retrofit-based network call
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

private fun parseDoubleSafe(value: String?): Double {
    if (value == null) return 0.0
    return try {
        value.toDouble()
    } catch (_: Exception) {
        value.replace("[^0-9.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
    }
}

// ------------------------------------------------------------
// Processing logic
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
// Compose UI Table Components
// ------------------------------------------------------------
@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .width(690.dp)
            .background(Color(0xFFE0F7FA))
            .padding(vertical = 2.dp)
    ) {
        repeat(6) { index ->
            TableCell(
                text = when (index) {
                    0 -> "Month"
                    1 -> "Name"
                    2 -> "Opening (₹)"
                    3 -> "Deposit (₹)"
                    4 -> "Expenditure (₹)"
                    else -> "Closing (₹)"
                },
                isHeader = true
            )
        }
    }
}

@Composable
fun BalanceRow(record: MonthlyMemberBalance, rowIndex: Int = 0) {
    val bg = if (rowIndex % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .width(690.dp)
            .background(bg)
    ) {
        TableCell(record.month)
        TableCell(record.name, textColor = Color(0xFF1565C0), fontWeight = FontWeight.Medium)
        TableCell("%.2f".format(record.opening), textColor = Color(0xFF374151))
        TableCell("%.2f".format(record.deposit), textColor = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
        TableCell("%.2f".format(record.expenses), textColor = Color(0xFFC62828))
        TableCell(
            "%.2f".format(record.closing),
            textColor = if (record.closing >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean = false,
    textColor: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
) {
    val finalColor = when {
        isHeader -> Color(0xFF002B49)
        textColor != Color.Unspecified -> textColor
        else -> Color.DarkGray
    }

    Box(
        modifier = Modifier
            .width(115.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = textAlign,
            fontSize = 13.sp,
            fontWeight = if (isHeader) FontWeight.Bold else fontWeight,
            color = finalColor,
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
        listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
    }

    val names = remember(summaryData) {
        summaryData.map { it.name }.distinct().sorted()
    }

    val filteredData = summaryData.filter { record ->
        val isFilterEmpty =
            filter.year.isBlank() &&
                    filter.month.isBlank() &&
                    filter.name.isBlank()

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

    val totalOpening = remember(filteredData) { filteredData.sumOf { it.opening } }
    val totalDeposit = remember(filteredData) { filteredData.sumOf { it.deposit } }
    val totalExpenses = remember(filteredData) { filteredData.sumOf { it.expenses } }
    val totalClosing = remember(filteredData) { filteredData.sumOf { it.closing } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Member Balance Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF002B49)
                )

                OutlinedButton(
                    onClick = { filter = BalanceFilter() },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Current Month", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Scrollable Table Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    // Filter Dropdown Header Row
                    FilterDropdownRow(
                        years = years,
                        months = months,
                        names = names,
                        filter = filter,
                        onFilterChange = { filter = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    TableHeader()

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (filteredData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(690.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No summary records match the selected filters.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        filteredData.forEachIndexed { index, record ->
                            BalanceRow(record, index)
                        }
                    }

                    // Total Footer Row
                    if (filteredData.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .width(690.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            TableCell("TOTAL", isHeader = true)
                            TableCell("${filteredData.size} rows", isHeader = true)
                            TableCell("%.2f".format(totalOpening), fontWeight = FontWeight.Bold, textColor = Color(0xFF374151))
                            TableCell("%.2f".format(totalDeposit), fontWeight = FontWeight.Bold, textColor = Color(0xFF1565C0))
                            TableCell("%.2f".format(totalExpenses), fontWeight = FontWeight.Bold, textColor = Color(0xFFC62828))
                            TableCell(
                                "%.2f".format(totalClosing),
                                fontWeight = FontWeight.Bold,
                                textColor = if (totalClosing >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
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
            .width(690.dp)
            .background(Color(0xFFF1F5F9))
            .padding(vertical = 4.dp, horizontal = 2.dp)
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

        repeat(3) {
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .padding(8.dp)
            )
        }
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
            .width(115.dp)
            .padding(horizontal = 2.dp)
    ) {
        OutlinedTextField(
            value = selectedOption.ifEmpty { "All $label" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.ifEmpty { "All $label" }, fontSize = 12.sp) },
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
// Date parsing helper
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
        }
    }
    return null
}
