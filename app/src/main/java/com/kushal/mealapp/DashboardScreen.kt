package com.kushal.mealapp

//noinspection UsingMaterialAndMaterial3Libraries
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Switch
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")

// ------------------------------------------------------------
// Main DashboardScreen (everything in one place)
// ------------------------------------------------------------
@Composable
fun DashboardScreen(
    combinedData: CombinedDataResult,
    monthlyBalances: List<MonthlyMemberBalance>
) {
    val totalMembers = combinedData.members.size
    val totalExpenditure = combinedData.transactions
        .filter { !it.Item.equals("Deposit", ignoreCase = true) }
        .sumOf { it.Price}
    val totalDeposit = combinedData.transactions
        .filter { it.Item.equals("Deposit", ignoreCase = false) }
        .sumOf { it.Price}

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
        Text(
            "Dashboard Summary",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- Summary card
        SummaryCard(totalMembers = totalMembers, totalExpenditure = totalExpenditure, totalDeposit = totalDeposit)

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 UPDATED MONTHLY EXPENDITURE SECTION
        MonthlyExpenditureSection(monthlyTotals)

        Spacer(Modifier.height(20.dp))


        // --- Monthly expenditure chart
        //Text("Monthly Expenditure", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
       // SimpleBarChart(data = monthlyTotals)

        Spacer(modifier = Modifier.height(20.dp))

        // --- Member list
        Text("Active Users", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        MembersListView(members = combinedData.members)

        Spacer(modifier = Modifier.height(20.dp))

        // --- Latest transactions summary
        if (combinedData.transactions.isNotEmpty()) {
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            RecentTransactionsList(transactions = combinedData.transactions.takeLast(10))
        } else {
            Text("No transactions available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        //month-wise data
        Text("Month & Year Expenditure Summary", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ExpandableMonthlySummaryList(transactions = combinedData.transactions)

        Spacer(Modifier.height(24.dp))

        Text("Monthly Deposits", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ExpandableMonthlyDepositList(transactions = combinedData.transactions)


        //InteractiveItemWisePieChart(transactions = combinedData.transactions)


        //LatestMonthItemWisePieChart(combinedData.transactions)

        MonthlyInteractiveItemWisePieChart(transactions=combinedData.transactions)

    }
    }
}

// ------------------------------------------------------------
// Summary Card (Top Info)
// ------------------------------------------------------------
@Composable
fun SummaryCard(totalMembers: Int, totalExpenditure: Double, totalDeposit:Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Members: $totalMembers", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Total Expenditure: ₹${"%.2f".format(totalExpenditure)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Total Deposit: ₹${"%.2f".format(totalDeposit)}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun MonthlyExpenditureSection(monthlyTotals: Map<YearMonth, Double>) {
    Text("Monthly Expenditure", style = MaterialTheme.typography.titleMedium)
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

    // ---------- FILTER ROW ----------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // MONTH
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

        // YEAR
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

    // TOGGLE
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Last 5")
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
        Text("All")
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
            onClick = { expanded = true }
        ) {
            Text(selected ?: label)
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


// ------------------------------------------------------------
// Simple Bar Chart (pure Compose, no libraries)
// ------------------------------------------------------------


@Composable
fun SimpleBarChart(data: Map<String, Double>) {
    val maxValue = data.values.maxOrNull() ?: 0.0
    val scrollState = rememberScrollState()

    // Remember animation trigger
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
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .height(24.dp)
                ) {
                    Text(
                        text = month,
                        modifier = Modifier.width(70.dp),
                        style = MaterialTheme.typography.bodySmall
                    )

                    // --- Animated Bar
                    Box(
                        modifier = Modifier
                            .width((animatedRatio * 300).dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "₹${"%.0f".format(value)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


// ------------------------------------------------------------
// Member List
// ------------------------------------------------------------
@Composable
fun MembersListView(members: List<NameRecord3>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        members.forEach {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(it.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (it.exit_dt.isEmpty()) "Active" else "Exited",
                    color = if (it.exit_dt.isEmpty())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ------------------------------------------------------------
// Recent Transactions
// ------------------------------------------------------------
@SuppressLint("RememberReturnType")
@Composable
fun RecentTransactionsList(transactions: List<TransactionRecord>) {

    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TransactionRecord?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        transactions.forEach { tx ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    // LEFT SIDE
                    Column {
                        Text(tx.Name, style = MaterialTheme.typography.bodyMedium)
                        Text(tx.Item, style = MaterialTheme.typography.bodySmall)
                    }

                    // RIGHT SIDE
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tx.Date, style = MaterialTheme.typography.bodySmall)

                        // CLICK AMOUNT → SHOW POPUP
                        Text(
                            "₹${"%.0f".format(tx.Price)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {

                                Log.d("EXP_DEBUG", "Clicked Expenditure = ${tx.Expenditure}")

                                selectedItem = tx      // set clicked transaction
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ---------- POPUP ----------
    if (showDialog && selectedItem != null) {

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Expenditure Details") },
            text = {
                Column {
                    Text("Name: ${selectedItem!!.Name}")
                    Text("Item: ${selectedItem!!.Item}")
                    Text("Amount: ₹${"%.0f".format(selectedItem!!.Price)}")
                    Text("Date: ${selectedItem!!.Date}")
                    Text("Expenditure: ${selectedItem!!.Expenditure}")
                }
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableMonthlySummaryList(transactions: List<TransactionRecord>) {

    // ---------------------------
    // Helper: Normalize date -> yyyy-MM
    // ---------------------------
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

    // Exclude deposits
    val expenditureTx = transactions.filterNot { it.Item.equals("Deposit", ignoreCase = true) }

    // Group by yyyy-MM
    val grouped = remember(expenditureTx) {
        expenditureTx.groupBy { normalizeToYearMonth(it.Date) }.toSortedMap()
    }

    // Months and Years
    val months = listOf(
        "01" to "January", "02" to "February", "03" to "March",
        "04" to "April", "05" to "May", "06" to "June",
        "07" to "July", "08" to "August", "09" to "September",
        "10" to "October", "11" to "November", "12" to "December"
    )

    val years = (2015..Calendar.getInstance().get(Calendar.YEAR)).toList().reversed()

    // dropdown states
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<String?>(null) }

    // Last 3 months default
    val currentDate = Calendar.getInstance()
    val last3Months = (0..2).map {
        val cal = (currentDate.clone() as Calendar).apply { add(Calendar.MONTH, -it) }
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    // Display groups according to Option A (selected month/year OR last 3 months)
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

    // ---------------------------
    // UI
    // ---------------------------
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {

        // Row with two exposed dropdowns (Month + Year)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            // MONTH Exposed Dropdown
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
                    modifier = Modifier.menuAnchor()
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

            // YEAR Exposed Dropdown
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
                    modifier = Modifier.menuAnchor()
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

        Spacer(modifier = Modifier.height(12.dp))

        // Clear filter
        if (selectedMonth != null || selectedYear != null) {
            TextButton(onClick = {
                selectedMonth = null
                selectedYear = null
            }) {
                Text("Clear Filter")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // The grouped display (only filtered months)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (displayGroups.isEmpty()) {
                // optional: show a helpful message when no data
                Text("No data for selected month/year", style = MaterialTheme.typography.bodySmall)
            }
            displayGroups.forEach { (yearMonth, txList) ->
                val total = txList.sumOf { it.Price }
                val parts = yearMonth.split("-")
                val monthName = months.find { it.first == parts.getOrNull(1) }?.second ?: "Unknown"
                val title = "$monthName ${parts.getOrNull(0)}"

                val isExpanded = expandedStates[yearMonth] ?: false

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = { expandedStates[yearMonth] = !isExpanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹${"%.0f".format(total)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
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
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(tx.Name, style = MaterialTheme.typography.bodyMedium)
                                        Text(tx.Item, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(tx.Date, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "₹${"%.0f".format(tx.Price)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                Log.d("EXP_DEBUG", "Clicked Expenditure = ${tx.Expenditure}")
                                                selectedItem = tx
                                                showDialog = true
                                            }
                                        )
                                    }
                                }
                                Divider(Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Popup dialog for details
    if (showDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("OK") }
            },
            title = { Text("Expenditure Details") },
            text = {
                Column {
                    Text("Name: ${selectedItem!!.Name}")
                    Text("Item: ${selectedItem!!.Item}")
                    Text("Amount: ₹${"%.0f".format(selectedItem!!.Price)}")
                    Text("Date: ${selectedItem!!.Date}")
                    Text("Expenditure: ${selectedItem!!.Expenditure}")
                }
            }
        )
    }
}

@Composable
fun ExpandableMonthlyDepositList(transactions: List<TransactionRecord>) {
    fun parseDate(dateStr: String): LocalDate? {
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy.MM.dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "dd.MM.yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply {
                    isLenient = true   // ⭐ important: prevents skipping
                }

                val date = sdf.parse(dateStr.trim()) ?: continue

                return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

            } catch (_: Exception) {}
        }
        return null
    }


    /* ---------------- ONLY DEPOSITS ---------------- */
    val depositTx = transactions.filter {
        it.Item.equals("Deposit", ignoreCase = true)
    }

    /* ---------------- PARSED DATA ---------------- */
    val parsedTx = remember(depositTx) {
        depositTx.mapNotNull { tx ->
            parseDate(tx.Date)?.let { date ->
                Triple(date, tx, "${date.year}-${"%02d".format(date.monthValue)}")
            }
        }
    }

    /* ---------------- FILTER VALUES ---------------- */
    val availableYears = parsedTx.map { it.first.year }.distinct().sortedDescending()
    val availableMonths = (1..12).toList()

    var selectedYear by remember { mutableStateOf<Int?>(null) }   // null = ALL
    var selectedMonth by remember { mutableStateOf<Int?>(null) }  // null = ALL

    /* ---------------- DEFAULT: LAST 3 MONTHS ---------------- */
    val last3YearMonths = remember {
        parsedTx.map { it.third }.distinct().sortedDescending().take(3)
    }

    /* ---------------- APPLY FILTER ---------------- */
    val filteredTx = parsedTx.filter { (date, _, ym) ->
        when {
            selectedYear == null && selectedMonth == null ->
                ym in last3YearMonths   // default last 3 months
            else ->
                (selectedYear == null || date.year == selectedYear) &&
                        (selectedMonth == null || date.monthValue == selectedMonth)
        }
    }

    /* ---------------- GROUP ---------------- */
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

        /* ================= FILTER ROW ================= */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            /* -------- MONTH FILTER -------- */
            var monthExpanded by remember { mutableStateOf(false) }

            Box {
                OutlinedButton(onClick = { monthExpanded = true }) {
                    Text(selectedMonth?.let { monthNumberToName(it) } ?: "All Months")
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

            /* -------- YEAR FILTER -------- */
            var yearExpanded by remember { mutableStateOf(false) }

            Box {
                OutlinedButton(onClick = { yearExpanded = true }) {
                    Text(selectedYear?.toString() ?: "All Years")
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

        /* ================= LIST ================= */
        grouped.forEach { (yearMonth, txList) ->

            val total = txList.sumOf { it.second.Price }
            val parts = yearMonth.split("-")
            val monthName = monthNumberToName(parts[1].toInt())
            val title = "$monthName ${parts[0]}"

            val isExpanded = expandedStates[yearMonth] ?: false

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { expandedStates[yearMonth] = !isExpanded }
            ) {
                Column(Modifier.padding(12.dp)) {

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "₹${"%.0f".format(total)}",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Icon(
                                imageVector = if (isExpanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
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
                                Text(tx.Name)
                                Text("₹${"%.0f".format(tx.Price)}")
                            }
                            Divider()
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

    // ❌ Remove Deposit
    val nonDepositTxns = transactions.filter {
        !it.Item.equals("Deposit", true)
    }

    // 📅 Extract year + month
    val parsedData = nonDepositTxns.mapNotNull {
        val ym = normalizeToYearMonth(it.Date)
        if (ym != null) Pair(ym, it) else null
    }

    val years = parsedData.map { it.first.first }.distinct().sortedDescending()
    val months = (1..12).toList()

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
        Text("No expenditure data available")
        return
    }

    // Dropdown states
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            // 🔵 YEAR DROPDOWN
            Box {
                OutlinedButton(onClick = { yearExpanded = true }) {
                    Text(selectedYear.toString())
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

            // 🟢 MONTH DROPDOWN
            Box {
                OutlinedButton(onClick = { monthExpanded = true }) {
                    Text(
                        listOf(
                            "January","February","March","April","May","June",
                            "July","August","September","October","November","December"
                        )[selectedMonth - 1]
                    )
                }

                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    listOf(
                                        "January","February","March","April","May","June",
                                        "July","August","September","October","November","December"
                                    )[month - 1]
                                )
                            },
                            onClick = {
                                selectedMonth = month
                                monthExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 🎯 FILTER DATA
        val filteredTxns = parsedData
            .filter {
                it.first.first == selectedYear &&
                        it.first.second == selectedMonth
            }
            .map { it.second }

        if (filteredTxns.isEmpty()) {
            Text("No data for selected month/year")
        } else {
            InteractiveItemWisePieChart(filteredTxns)
        }
    }
}

@Composable
fun InteractiveItemWisePieChart(transactions: List<TransactionRecord>) {
    // --- Filter out deposits ---
    val filtered = transactions.filter { it.Item.lowercase() != "deposit" }
    val grouped = filtered.groupBy { it.Item }
    val itemSums = grouped.mapValues { (_, list) -> list.sumOf { it.Price } }

    val total = itemSums.values.sum()
    if (total == 0.0) {
        Text("No expenditure data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    // --- Animation setup ---
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val animatedSweep by animateFloatAsState(
        targetValue = if (startAnimation) 360f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
    )

    var selectedIndex by remember { mutableStateOf(-1) }

    // --- Colors ---
    val colors = listOf(
        Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFA726),
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFFFF7043),
        Color(0xFF26C6DA), Color(0xFFD4E157)
    )

    val sortedItems = itemSums.entries.sortedByDescending { it.value }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        var startAngle = 0f
                        val radius = min(size.width, size.height) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)

                        if (distance <= radius) {
                            var angle = (atan2(dy, dx) * (180f / PI.toFloat()))
                            angle = (angle + 360f + 90f) % 360f

                            sortedItems.forEachIndexed { index, entry ->
                                val sweepAngle = (entry.value / total * 360f).toFloat()
                                if (angle in startAngle..(startAngle + sweepAngle)) {
                                    selectedIndex = if (selectedIndex == index) -1 else index
                                    return@detectTapGestures
                                }
                                startAngle += sweepAngle
                            }
                        }
                    }
                }
        ) {
            var startAngle = 0f

            sortedItems.forEachIndexed { index, (item, value) ->
                val sweepAngle = ((value / total) * animatedSweep).toFloat()
                val isSelected = selectedIndex == index
                val sliceColor = colors[index % colors.size]

                // Offset outward for selected slice (Float-safe)
                val offsetRadius = if (isSelected) 12.dp.toPx() else 0f
                val angleMid = startAngle + sweepAngle / 2f
                val angleRad = Math.toRadians(angleMid.toDouble()).toFloat()
                val offsetX = cos(angleRad) * offsetRadius
                val offsetY = sin(angleRad) * offsetRadius

                withTransform({
                    translate(left = offsetX, top = offsetY)
                }) {
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                }
                startAngle += sweepAngle
            }
        }

        // --- Center Info when selected ---
        if (selectedIndex >= 0) {
            val entry = sortedItems[selectedIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
                    .padding(12.dp)
            ) {
                Text(entry.key, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "₹${"%.0f".format(entry.value)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${((entry.value / total) * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    // --- Legend ---
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp, start = 16.dp)
    ) {
        sortedItems.forEachIndexed { index, (item, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(colors[index % colors.size], CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$item: ₹${"%.0f".format(value)} (${((value / total) * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
@Composable
fun LatestMonthItemWisePieChart(transactions: List<TransactionRecord>) {
    // --- Helper: normalize date to yyyy-MM ---
    fun normalizeToYearMonth(dateStr: String): String {
        val possibleFormats = listOf(
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
            "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy"
        )
        for (fmt in possibleFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val parsed = sdf.parse(dateStr)
                if (parsed != null) {
                    return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(parsed)
                }
            } catch (_: Exception) { }
        }
        return dateStr.take(7)
    }

    // --- Filter out deposits and invalid dates ---
    val validTransactions = transactions
        .filter { it.Item.lowercase() != "deposit" && it.Date.isNotBlank() }
        .mapNotNull { tx ->
            val ym = normalizeToYearMonth(tx.Date)
            if (ym.length == 7) ym to tx else null
        }

    if (validTransactions.isEmpty()) {
        Text(
            "No valid expenditure data available",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // --- Group by yyyy-MM ---
    val grouped = validTransactions.groupBy { it.first }.mapValues { it.value.map { v -> v.second } }

    // --- Find latest month (yyyy-MM) ---
    val latestMonth = grouped.keys.maxOrNull()
    val latestMonthTransactions = grouped[latestMonth] ?: emptyList()

    // --- Display month name ---
    val parts = latestMonth?.split("-") ?: listOf()
    val monthName = when (parts.getOrNull(1)?.toIntOrNull()) {
        1 -> "January"; 2 -> "February"; 3 -> "March"
        4 -> "April"; 5 -> "May"; 6 -> "June"
        7 -> "July"; 8 -> "August"; 9 -> "September"
        10 -> "October"; 11 -> "November"; 12 -> "December"
        else -> "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Item-wise Expenditure for $monthName ${parts.getOrNull(0) ?: ""}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (latestMonthTransactions.isEmpty()) {
            Text(
                "No expenditure data for $monthName ${parts.getOrNull(0) ?: ""}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            InteractiveItemWisePieChart(latestMonthTransactions)
        }
    }
}
