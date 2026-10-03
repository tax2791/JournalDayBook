package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

// ------------------------------------------------------------
// Main DashboardScreen
// ------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    combinedData: CombinedDataResult,
    monthlyBalances: List<MonthlyMemberBalance>
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    val totalMembers = combinedData.members.size
    val totalExpenditure = combinedData.transactions
        .filter { !it.Item.equals("Deposit", ignoreCase = true) }
        .sumOf { it.Price }
    val totalDeposit = combinedData.transactions
        .filter { it.Item.equals("Deposit", ignoreCase = false) }
        .sumOf { it.Price }

    // Group total expenditure per month
    val monthlyTotals = remember(combinedData.transactions) {
        fun parseToYearMonth(dateStr: String): YearMonth? {
            val formats = listOf(
                "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
                "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy"
            )
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                    val parsed = sdf.parse(dateStr) ?: continue
                    return parsed.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .let { YearMonth.of(it.year, it.month) }
                } catch (_: Exception) {}
            }
            return null
        }

        combinedData.transactions
            .filterNot { it.Item.equals("Deposit", ignoreCase = true) }
            .mapNotNull { tx ->
                parseToYearMonth(tx.Date)?.let { ym -> ym to tx.Price }
            }
            .groupBy { it.first }
            .mapValues { (_, list) -> list.sumOf { it.second } }
            .toSortedMap(compareByDescending { it })
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = Color(0xFFF4F6F9),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics Dashboard",
                        color = Color(0xFF002B49),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Summary Metric Cards
            SummaryCard(totalMembers = totalMembers, totalExpenditure = totalExpenditure, totalDeposit = totalDeposit)

            // 2. Monthly Expenditure Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    MonthlyExpenditureSection(monthlyTotals)
                }
            }

            // 3. Category Interactive Pie Chart Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("📊 Category Expense Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF002B49))
                    Spacer(modifier = Modifier.height(10.dp))
                    MonthlyInteractiveItemWisePieChart(transactions = combinedData.transactions)
                }
            }

            // 4. Registered Users Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("👥 Registered Members & Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF002B49))
                    Spacer(modifier = Modifier.height(10.dp))
                    MembersListView(members = combinedData.members)
                }
            }

            // 5. Recent Transactions Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🕒 Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF002B49))
                    Spacer(modifier = Modifier.height(10.dp))
                    if (combinedData.transactions.isNotEmpty()) {
                        RecentTransactionsList(transactions = combinedData.transactions.takeLast(10).reversed())
                    } else {
                        Text("No transactions available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 6. Expandable Monthly Summary List
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("📅 Monthly Expenditure Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF002B49))
                    Spacer(modifier = Modifier.height(10.dp))
                    ExpandableMonthlySummaryList(transactions = combinedData.transactions)
                }
            }

            // 7. Expandable Monthly Deposit List
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💰 Monthly Deposit Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF002B49))
                    Spacer(modifier = Modifier.height(10.dp))
                    ExpandableMonthlyDepositList(transactions = combinedData.transactions)
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Summary Cards
// ------------------------------------------------------------
@Composable
fun SummaryCard(totalMembers: Int, totalExpenditure: Double, totalDeposit: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("👥 Members", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Spacer(modifier = Modifier.height(4.dp))
                Text("$totalMembers", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
            }
        }

        Card(
            modifier = Modifier.weight(1.1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("💸 Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${"%.0f".format(totalExpenditure)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }
        }

        Card(
            modifier = Modifier.weight(1.1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("💰 Deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${"%.0f".format(totalDeposit)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            }
        }
    }
}

@Composable
fun MonthlyExpenditureSection(monthlyTotals: Map<YearMonth, Double>) {
    Text("Monthly Expenditure Chart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF002B49))
    Spacer(Modifier.height(8.dp))

    val months = listOf(
        1 to "January", 2 to "February", 3 to "March",
        4 to "April", 5 to "May", 6 to "June",
        7 to "July", 8 to "August", 9 to "September",
        10 to "October", 11 to "November", 12 to "December"
    )

    val years = remember(monthlyTotals) {
        monthlyTotals.keys.map { it.year }.distinct().sortedDescending()
    }

    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var showAll by remember { mutableStateOf(false) }

    val filteredData = remember(
        selectedMonth, selectedYear, showAll, monthlyTotals
    ) {
        when {
            showAll -> monthlyTotals

            selectedMonth != null && selectedYear != null ->
                monthlyTotals.filterKeys {
                    it.monthValue == selectedMonth &&
                            it.year == selectedYear
                }

            else -> monthlyTotals.entries.take(5).associate { it.key to it.value }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SimpleDropdown(
                label = "Month",
                selected = selectedMonth?.let { months.first { m -> m.first == it }.second },
                options = months.map { it.second }
            ) { index ->
                selectedMonth = months[index].first
                showAll = false
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            SimpleDropdown(
                label = "Year",
                selected = selectedYear?.toString(),
                options = years.map { it.toString() }
            ) { index ->
                selectedYear = years[index]
                showAll = false
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Recent 5", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.width(6.dp))
        Switch(
            checked = showAll,
            onCheckedChange = {
                showAll = it
                if (it) {
                    selectedMonth = null
                    selectedYear = null
                }
            }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("All Months", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
    }

    Spacer(Modifier.height(8.dp))

    AnimatedContent(
        targetState = filteredData,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "Chart"
    ) { data ->
        SimpleBarChart(
            data = data.mapKeys {
                "${months.first { m -> m.first == it.key.monthValue }.second}-${it.key.year}"
            }
        )
    }
}

@Composable
fun SimpleDropdown(
    label: String,
    selected: String?,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            onClick = { expanded = true }
        ) {
            Text(selected ?: label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(data: Map<String, Double>) {
    val maxValue = data.values.maxOrNull() ?: 0.0
    val scrollState = rememberScrollState()

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    Column(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        Column {
            data.forEach { (month, value) ->
                val ratio = if (maxValue == 0.0) 0f else (value / maxValue).toFloat()
                val animatedRatio by animateFloatAsState(
                    targetValue = if (startAnimation) ratio else 0f,
                    animationSpec = tween(durationMillis = 800)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .height(24.dp)
                ) {
                    Text(
                        text = month,
                        modifier = Modifier.width(85.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .width((animatedRatio * 260).dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1565C0))
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "₹${"%.0f".format(value)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun MembersListView(members: List<NameRecord3>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        members.forEach {
            val isActive = it.exit_dt.isEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Color(0xFFE3F2FD) else Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it.name.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color(0xFF1565C0) else Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(it.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isActive) "Active" else "Exited",
                        color = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun RecentTransactionsList(transactions: List<TransactionRecord>) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TransactionRecord?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        transactions.forEach { tx ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedItem = tx
                        showDialog = true
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tx.Name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Text(tx.Item, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "₹${"%.0f".format(tx.Price)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.Item.equals("Deposit", ignoreCase = true)) Color(0xFF1565C0) else Color(0xFF2E7D32)
                        )
                        Text(tx.Date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Expenditure Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Name: ${selectedItem!!.Name}", fontWeight = FontWeight.Bold)
                    Text("Item: ${selectedItem!!.Item}")
                    Text("Amount: ₹${"%.2f".format(selectedItem!!.Price)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Date: ${selectedItem!!.Date}")
                    Text("Remark: ${selectedItem!!.Expenditure}")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableMonthlySummaryList(transactions: List<TransactionRecord>) {
    fun normalizeToYearMonth(dateStr: String): String {
        val possibleFormats = listOf(
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
            "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy"
        )
        for (fmt in possibleFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val parsed: Date = sdf.parse(dateStr) ?: continue
                return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(parsed)
            } catch (_: Exception) { }
        }
        return dateStr.take(7)
    }

    val expenditureTx = transactions.filterNot { it.Item.equals("Deposit", ignoreCase = true) }

    val grouped = remember(expenditureTx) {
        expenditureTx.groupBy { normalizeToYearMonth(it.Date) }.toSortedMap()
    }

    val months = listOf(
        "01" to "January", "02" to "February", "03" to "March",
        "04" to "April", "05" to "May", "06" to "June",
        "07" to "July", "08" to "August", "09" to "September",
        "10" to "October", "11" to "November", "12" to "December"
    )

    val years = (2015..Calendar.getInstance().get(Calendar.YEAR)).toList().reversed()

    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<String?>(null) }

    val currentDate = Calendar.getInstance()
    val last3Months = (0..2).map {
        val cal = (currentDate.clone() as Calendar).apply { add(Calendar.MONTH, -it) }
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    val displayGroups = remember(selectedMonth, selectedYear, grouped) {
        if (!selectedMonth.isNullOrBlank() && !selectedYear.isNullOrBlank()) {
            val target = "${selectedYear}-${selectedMonth!!.padStart(2, '0')}"
            grouped.filterKeys { it == target }
        } else {
            grouped.filterKeys { it in last3Months }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TransactionRecord?>(null) }
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expandedMonth,
                onExpandedChange = { expandedMonth = !expandedMonth },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = months.find { it.first == selectedMonth }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                    placeholder = { Text("Select Month") },
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedMonth,
                    onDismissRequest = { expandedMonth = false }
                ) {
                    months.forEach { (num, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedMonth = num
                                expandedMonth = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedYear,
                onExpandedChange = { expandedYear = !expandedYear },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = selectedYear ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Year") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                    placeholder = { Text("Select Year") },
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedYear,
                    onDismissRequest = { expandedYear = false }
                ) {
                    years.forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = {
                                selectedYear = y.toString()
                                expandedYear = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedMonth != null || selectedYear != null) {
            TextButton(onClick = {
                selectedMonth = null
                selectedYear = null
            }) {
                Text("Clear Filter", color = Color(0xFF1565C0))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (displayGroups.isEmpty()) {
                Text("No expenditure data for selected month/year", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            displayGroups.forEach { (yearMonth, txList) ->
                val total = txList.sumOf { it.Price }
                val parts = yearMonth.split("-")
                val monthName = months.find { it.first == parts.getOrNull(1) }?.second ?: "Unknown"
                val title = "$monthName ${parts.getOrNull(0)}"

                val isExpanded = expandedStates[yearMonth] ?: false

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedStates[yearMonth] = !isExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹${"%.0f".format(total)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(Modifier.height(8.dp))
                            txList.forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(tx.Name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(tx.Item, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(tx.Date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(
                                            "₹${"%.0f".format(tx.Price)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1565C0),
                                            modifier = Modifier.clickable {
                                                selectedItem = tx
                                                showDialog = true
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("OK") }
            },
            title = { Text("Expenditure Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Name: ${selectedItem!!.Name}", fontWeight = FontWeight.Bold)
                    Text("Item: ${selectedItem!!.Item}")
                    Text("Amount: ₹${"%.2f".format(selectedItem!!.Price)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Date: ${selectedItem!!.Date}")
                    Text("Remark: ${selectedItem!!.Expenditure}")
                }
            }
        )
    }
}

@Composable
fun ExpandableMonthlyDepositList(transactions: List<TransactionRecord>) {
    fun parseDate(dateStr: String): LocalDate? {
        val formats = listOf(
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
            "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy",
            "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "yyyy/MM/dd HH:mm:ss"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply {
                    isLenient = true
                }
                val date = sdf.parse(dateStr.trim()) ?: continue
                return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (_: Exception) {}
        }
        return null
    }

    val depositTx = transactions.filter {
        it.Item.equals("Deposit", ignoreCase = true)
    }

    val parsedTx = remember(depositTx) {
        depositTx.mapNotNull { tx ->
            parseDate(tx.Date)?.let { date ->
                Triple(date, tx, "${date.year}-${"%02d".format(date.monthValue)}")
            }
        }
    }

    val availableYears = parsedTx.map { it.first.year }.distinct().sortedDescending()
    val availableMonths = (1..12).toList()

    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    val last3YearMonths = remember {
        parsedTx.map { it.third }.distinct().sortedDescending().take(3)
    }

    val filteredTx = parsedTx.filter { (date, _, ym) ->
        when {
            selectedYear == null && selectedMonth == null ->
                ym in last3YearMonths
            else ->
                (selectedYear == null || date.year == selectedYear) &&
                        (selectedMonth == null || date.monthValue == selectedMonth)
        }
    }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun monthNumberToName(month: Int): String =
        monthNames.getOrNull(month - 1) ?: "Unknown"

    val grouped = filteredTx
        .groupBy { it.third }
        .toSortedMap(compareByDescending { it })

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var monthExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { monthExpanded = true }
                ) {
                    Text(selectedMonth?.let { monthNumberToName(it) } ?: "All Months", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Months") },
                        onClick = {
                            selectedMonth = null
                            monthExpanded = false
                        }
                    )
                    availableMonths.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(monthNumberToName(m)) },
                            onClick = {
                                selectedMonth = m
                                monthExpanded = false
                            }
                        )
                    }
                }
            }

            var yearExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { yearExpanded = true }
                ) {
                    Text(selectedYear?.toString() ?: "All Years", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Years") },
                        onClick = {
                            selectedYear = null
                            yearExpanded = false
                        }
                    )
                    availableYears.forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = {
                                selectedYear = y
                                yearExpanded = false
                            }
                        )
                    }
                }
            }
        }

        grouped.forEach { (yearMonth, txList) ->
            val total = txList.sumOf { it.second.Price }
            val parts = yearMonth.split("-")
            val monthName = monthNumberToName(parts[1].toInt())
            val title = "$monthName ${parts[0]}"

            val isExpanded = expandedStates[yearMonth] ?: false

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedStates[yearMonth] = !isExpanded },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "₹${"%.0f".format(total)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(8.dp))
                        txList.forEach { (_, tx, _) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tx.Name, style = MaterialTheme.typography.bodyMedium)
                                Text("₹${"%.0f".format(tx.Price)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyInteractiveItemWisePieChart(transactions: List<TransactionRecord>) {
    fun normalizeToYearMonth(dateStr: String): Pair<Int, Int>? {
        val possibleFormats = listOf(
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
            "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy"
        )
        for (fmt in possibleFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val parsed = sdf.parse(dateStr)
                if (parsed != null) {
                    val cal = Calendar.getInstance()
                    cal.time = parsed
                    return Pair(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1
                    )
                }
            } catch (_: Exception) {}
        }
        return null
    }

    val nonDepositTxns = transactions.filter {
        !it.Item.equals("Deposit", true)
    }

    val parsedData = nonDepositTxns.mapNotNull {
        val ym = normalizeToYearMonth(it.Date)
        if (ym != null) Pair(ym, it) else null
    }

    val years = parsedData.map { it.first.first }.distinct().sortedDescending()

    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

    var selectedYear by remember {
        mutableStateOf(
            if (years.contains(currentYear))
                currentYear
            else
                years.firstOrNull() ?: 0
        )
    }

    var selectedMonth by remember {
        val availableMonthsForYear = parsedData
            .filter { it.first.first == selectedYear }
            .map { it.first.second }
            .distinct()

        mutableStateOf(
            when {
                availableMonthsForYear.contains(currentMonth) -> currentMonth
                availableMonthsForYear.isNotEmpty() -> availableMonthsForYear.maxOrNull()!!
                else -> 1
            }
        )
    }

    if (years.isEmpty()) {
        Text("No expenditure data available for chart", color = Color.Gray)
        return
    }

    val activeMonthTxns = parsedData.filter {
        it.first.first == selectedYear && it.first.second == selectedMonth
    }.map { it.second }

    val itemPriceMap = remember(activeMonthTxns) {
        activeMonthTxns.groupBy { it.Item }
            .mapValues { entry -> entry.value.sumOf { it.Price } }
            .filter { it.value > 0 }
    }

    val chartData = remember(itemPriceMap) {
        itemPriceMap.toList().sortedByDescending { it.second }
    }

    val colors = remember {
        listOf(
            Color(0xFF1E88E5), Color(0xFFF57C00), Color(0xFF43A047), Color(0xFF8E24AA),
            Color(0xFFD81B60), Color(0xFF00ACC1), Color(0xFF3949AB), Color(0xFF7CB342),
            Color(0xFFFB8C00), Color(0xFF00897B)
        )
    }

    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { monthExpanded = true }
                ) {
                    Text(monthsList.getOrNull(selectedMonth - 1) ?: "Month", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    (1..12).forEach { m ->
                        DropdownMenuItem(
                            text = { Text(monthsList.getOrNull(m - 1) ?: m.toString()) },
                            onClick = {
                                selectedMonth = m
                                monthExpanded = false
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { yearExpanded = true }
                ) {
                    Text(selectedYear.toString(), fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                selectedYear = year
                                yearExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (chartData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category expense data for ${monthsList.getOrNull(selectedMonth - 1)} $selectedYear",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            val totalExpense = remember(chartData) { chartData.sumOf { it.second } }
            var selectedIndex by remember { mutableStateOf<Int?>(null) }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SIDE: Small Touchable Pie Chart Canvas (140.dp)
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(140.dp)
                                .pointerInput(chartData, totalExpense) {
                                    detectTapGestures { tapOffset ->
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val dx = tapOffset.x - centerX
                                        val dy = tapOffset.y - centerY
                                        val distance = sqrt(dx * dx + dy * dy)
                                        val radius = min(centerX, centerY)

                                        if (distance <= radius && totalExpense > 0) {
                                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                            if (angle < 0) angle += 360f

                                            var cumulativeAngle = 0f
                                            chartData.forEachIndexed { index, (_, value) ->
                                                val sweep = (value / totalExpense * 360).toFloat()
                                                if (angle >= cumulativeAngle && angle < cumulativeAngle + sweep) {
                                                    selectedIndex = if (selectedIndex == index) null else index
                                                    return@detectTapGestures
                                                }
                                                cumulativeAngle += sweep
                                            }
                                        }
                                    }
                                }
                        ) {
                            var startAngle = 0f
                            chartData.forEachIndexed { index, (_, value) ->
                                val sweepAngle = (value / totalExpense * 360).toFloat()
                                val isSelected = selectedIndex == index

                                drawArc(
                                    color = colors[index % colors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    style = Fill,
                                    alpha = if (selectedIndex == null || isSelected) 1f else 0.45f
                                )

                                if (isSelected) {
                                    drawArc(
                                        color = Color.White,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = true,
                                        style = Stroke(width = 5f)
                                    )
                                }
                                startAngle += sweepAngle
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // RIGHT SIDE: Item Detail List with Percentages
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chartData.forEachIndexed { index, (item, value) ->
                            val percentage = (value / totalExpense * 100)
                            val isSelected = selectedIndex == index
                            val color = colors[index % colors.size]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF8F9FA)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(color, shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color(0xFF1F2937)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${"%.0f".format(value)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "${"%.1f".format(percentage)}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1565C0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Detail Flash Card
                selectedIndex?.let { index ->
                    val (selectedItem, selectedValue) = chartData[index]
                    val percentage = (selectedValue / totalExpense * 100)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002B49)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(colors[index % colors.size], CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Category: $selectedItem",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Spent: ₹${"%.2f".format(selectedValue)}",
                                        color = Color(0xFF80D8FF),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${"%.1f".format(percentage)}%",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
