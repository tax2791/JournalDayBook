@file:OptIn(ExperimentalFoundationApi::class)
package database

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.platform.LocalContext
import com.kushal.mealapp.BannerAdView
import com.kushal.mealapp.FormStyleCascadingCalendar
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class ReportViewFormat { TABLE, CARDS }

data class MemberReportData(
    val member: Member,
    val openingBalance: Double,
    val transferIn: Double,
    val expenditure: Double,
    val transferOut: Double,
    val closingBalance: Double
)

fun calculateMemberReport(
    member: Member,
    allMeals: List<Meal1>,
    fromDateObj: Date,
    toDateObj: Date
): MemberReportData {
    val memberMeals = allMeals.filter { it.name == member.name }

    // Prior transactions before fromDateObj (Receipts and Deposits count as Transfer In)
    val priorMeals = memberMeals.filter { it.date.time < fromDateObj.time }
    val priorTransferIn = priorMeals.filter {
        it.item.startsWith("Receipt", ignoreCase = true) || it.item in listOf("Deposit", "Transfer In")
    }.sumOf { it.price }
    val priorExpenditure = priorMeals.filter {
        !it.item.startsWith("Receipt", ignoreCase = true) && it.item !in listOf("Transfer In", "Transfer Out", "Deposit")
    }.sumOf { it.price }
    val priorTransferOut = priorMeals.filter { it.item == "Transfer Out" }.sumOf { abs(it.price) }

    // Base opening balance: If created AFTER toDateObj, balance is 0.0 for this period
    val baseOpeningBalance = if (member.joinDate != null && member.joinDate.time > toDateObj.time) {
        0.0
    } else {
        member.openingBalance
    }

    // Opening balance at start of period = baseOpeningBalance + net prior transactions
    val effectiveOpeningBalance = baseOpeningBalance + priorTransferIn - priorExpenditure - priorTransferOut

    // Prevailing transactions during filter period [fromDateObj .. toDateObj]
    val periodMeals = memberMeals.filter { it.date.time >= fromDateObj.time && it.date.time <= toDateObj.time }
    val transferIn = periodMeals.filter {
        it.item.startsWith("Receipt", ignoreCase = true) || it.item in listOf("Deposit", "Transfer In")
    }.sumOf { it.price }
    val expenditure = periodMeals.filter {
        !it.item.startsWith("Receipt", ignoreCase = true) && it.item !in listOf("Transfer In", "Transfer Out", "Deposit")
    }.sumOf { it.price }
    val transferOut = periodMeals.filter { it.item == "Transfer Out" }.sumOf { abs(it.price) }

    val closingBalance = effectiveOpeningBalance + transferIn - expenditure - transferOut

    return MemberReportData(
        member = member,
        openingBalance = effectiveOpeningBalance,
        transferIn = transferIn,
        expenditure = expenditure,
        transferOut = transferOut,
        closingBalance = closingBalance
    )
}

@Composable
fun MemberReportScreen(viewModel: MealViewModel) {
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())
    val allMeals by viewModel.allMeals1.observeAsState(emptyList())

    // State for filtering and formatting
    var selectedFormat by remember { mutableStateOf(ReportViewFormat.TABLE) }
    var selectedColumns by remember { mutableStateOf(setOf("Op. Bal", "Cl. Bal")) }
    val availableColumns = listOf("Op. Bal", "Tr. In", "Exp.", "Tr. Out", "Cl. Bal")

    val currentCal = remember { Calendar.getInstance() }
    var fromYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var fromMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }
    var fromDay by remember { mutableIntStateOf(1) }
    
    var toYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var toMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }
    var toDay by remember { mutableIntStateOf(currentCal.get(Calendar.DAY_OF_MONTH)) }
    
    var activePicker by remember { mutableStateOf<String?>(null) } // "from" or "to"

    val fromDateObj = remember(fromYear, fromMonth, fromDay) {
        Calendar.getInstance().apply {
            set(fromYear, fromMonth, fromDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    val toDateObj = remember(toYear, toMonth, toDay) {
        Calendar.getInstance().apply {
            set(toYear, toMonth, toDay, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    // Active members based on joinDate and exitDate
    val activeMembers = remember(allMembers, fromDateObj, toDateObj) {
        allMembers.filter { member ->
            val jDate = member.joinDate
            val eDate = member.exitDate
            (jDate == null || jDate.time <= toDateObj.time) && (eDate == null || eDate.time >= fromDateObj.time)
        }
    }

    val reportDataList = remember(activeMembers, allMeals, fromDateObj, toDateObj) {
        activeMembers.map { member ->
            calculateMemberReport(member, allMeals, fromDateObj, toDateObj)
        }
    }

    val relatedTransactions = remember(allMeals, activeMembers, fromDateObj, toDateObj) {
        val activeNames = activeMembers.map { it.name }.toSet()
        allMeals.filter { meal ->
            meal.name in activeNames && meal.date.time >= fromDateObj.time && meal.date.time <= toDateObj.time
        }.sortedBy { it.date }
    }

    val context = LocalContext.current
    val fromDateStr = "$fromDay/${fromMonth + 1}/$fromYear"
    val toDateStr = "$toDay/${toMonth + 1}/$toYear"

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            // Header Title & Export Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Calculator Button
                    IconButton(
                        onClick = { com.kushal.mealapp.FloatingCalculatorState.show() }
                    ) {
                        Text("🧮", fontSize = 18.sp)
                    }

                    // Export PDF Button
                    Button(
                        onClick = {
                            if (reportDataList.isEmpty()) {
                                Toast.makeText(context, "No report data to export", Toast.LENGTH_SHORT).show()
                            } else {
                                val pdfFile = MemberReportExporter.exportToPdf(
                                    context, reportDataList, relatedTransactions, fromDateStr, toDateStr
                                )
                                if (pdfFile != null) {
                                    Toast.makeText(context, "PDF Report Exported ✅", Toast.LENGTH_SHORT).show()
                                    MemberReportExporter.shareOrOpenFile(
                                        context, pdfFile, "application/pdf", "Share PDF Report"
                                    )
                                } else {
                                    Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Export Excel CSV Button
                    Button(
                        onClick = {
                            if (reportDataList.isEmpty()) {
                                Toast.makeText(context, "No report data to export", Toast.LENGTH_SHORT).show()
                            } else {
                                val csvFile = MemberReportExporter.exportToExcelCsv(
                                    context, reportDataList, relatedTransactions, fromDateStr, toDateStr
                                )
                                if (csvFile != null) {
                                    Toast.makeText(context, "Excel CSV Exported ✅", Toast.LENGTH_SHORT).show()
                                    MemberReportExporter.shareOrOpenFile(
                                        context, csvFile, "text/csv", "Share Excel CSV Report"
                                    )
                                } else {
                                    Toast.makeText(context, "Failed to export Excel CSV", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Excel", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Control Panel
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Date Filter Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("From:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(48.dp))
                        OutlinedButton(
                            onClick = { activePicker = if (activePicker == "from") null else "from" }, 
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("$fromDay/${fromMonth + 1}/$fromYear", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.DateRange, contentDescription = "Select From Date", modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("To:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(28.dp))
                        OutlinedButton(
                            onClick = { activePicker = if (activePicker == "to") null else "to" }, 
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("$toDay/${toMonth + 1}/$toYear", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.DateRange, contentDescription = "Select To Date", modifier = Modifier.size(16.dp))
                        }
                    }

                    if (activePicker != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isFrom = activePicker == "from"
                        val selYear = if (isFrom) fromYear else toYear
                        val selMonth = if (isFrom) fromMonth else toMonth
                        val selDay = if (isFrom) fromDay else toDay

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFrom) "Select From Date" else "Select To Date",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = { activePicker = null }) {
                                        Text("Close")
                                    }
                                }

                                FormStyleCascadingCalendar(
                                    year = selYear,
                                    month = selMonth,
                                    selectedDay = selDay,
                                    onDateSelected = { y, m, d ->
                                        if (isFrom) {
                                            fromYear = y
                                            fromMonth = m
                                            fromDay = d
                                        } else {
                                            toYear = y
                                            toMonth = m
                                            toDay = d
                                        }
                                        activePicker = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // View Format Selection (Table, Vertical Cards, Chart)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View Format:", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReportViewFormat.entries.forEach { format ->
                                InputChip(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format },
                                    label = { Text(format.name.replace("_", " ")) }
                                )
                            }
                        }
                    }

                    // Column Selector (Only visible for Table View)
                    AnimatedVisibility(visible = selectedFormat == ReportViewFormat.TABLE) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Select Columns:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableColumns.forEach { col ->
                                    FilterChip(
                                        selected = selectedColumns.contains(col),
                                        onClick = {
                                            selectedColumns = if (selectedColumns.contains(col)) {
                                                selectedColumns - col
                                            } else {
                                                selectedColumns + col
                                            }
                                        },
                                        label = { Text(col) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Area with Format Switcher
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = selectedFormat, label = "ViewFormatAnimation") { format ->
                    when (format) {
                        ReportViewFormat.TABLE -> TableView(reportDataList, allMeals, fromDateObj, toDateObj, selectedColumns)
                        ReportViewFormat.CARDS -> VerticalCardView(reportDataList, allMeals, fromDateObj, toDateObj)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BannerAdView()
        }
    }
}

@Composable
fun TableView(
    reportDataList: List<MemberReportData>,
    allMeals: List<Meal1>,
    fromDateObj: Date,
    toDateObj: Date,
    selectedColumns: Set<String>
) {
    val horizontalScrollState = rememberScrollState()
    var expandedMemberId by remember { mutableStateOf<Long?>(null) }

    val totalOp = remember(reportDataList) { reportDataList.sumOf { it.openingBalance } }
    val totalTrIn = remember(reportDataList) { reportDataList.sumOf { it.transferIn } }
    val totalExp = remember(reportDataList) { reportDataList.sumOf { it.expenditure } }
    val totalTrOut = remember(reportDataList) { reportDataList.sumOf { it.transferOut } }
    val totalCl = remember(reportDataList) { reportDataList.sumOf { it.closingBalance } }

    val showOpBal = selectedColumns.contains("Op. Bal")
    val showTrIn = selectedColumns.contains("Tr. In")
    val showExp = selectedColumns.contains("Exp.")
    val showTrOut = selectedColumns.contains("Tr. Out")
    val showClBal = selectedColumns.contains("Cl. Bal")

    val visibleColsCount = 1 +
            (if (showOpBal) 1 else 0) +
            (if (showTrIn) 1 else 0) +
            (if (showExp) 1 else 0) +
            (if (showTrOut) 1 else 0) +
            (if (showClBal) 1 else 0)

    val nameWidth = 120.dp
    val colWidth = 80.dp
    val totalWidth = nameWidth + (colWidth * (visibleColsCount - 1))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        LazyColumn(
            modifier = Modifier
                .width(totalWidth.coerceAtLeast(320.dp))
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 8.dp)
                ) {
                    ReportTableCell("Name", nameWidth, Color.White, true)
                    if (showOpBal) ReportTableCell("Op. Bal", colWidth, Color.White, true)
                    if (showTrIn) ReportTableCell("Tr. In", colWidth, Color.White, true)
                    if (showExp) ReportTableCell("Exp.", colWidth, Color.White, true)
                    if (showTrOut) ReportTableCell("Tr. Out", colWidth, Color.White, true)
                    if (showClBal) ReportTableCell("Cl. Bal", colWidth, Color.White, true)
                }
            }

            itemsIndexed(reportDataList) { index, data ->
                val member = data.member
                val openingBalance = data.openingBalance
                val transferIn = data.transferIn
                val expenditure = data.expenditure
                val transferOut = data.transferOut
                val closingBalance = data.closingBalance

                val isExpanded = expandedMemberId == member.id

                val backgroundColor = if (index % 2 == 0) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .height(IntrinsicSize.Min)
                    ) {
                        ReportTableCell(
                            text = "${member.name} ${if (isExpanded) "▲" else "▼"}",
                            width = nameWidth,
                            textColor = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            onClick = {
                                expandedMemberId = if (isExpanded) null else member.id
                            }
                        )
                        if (showOpBal) ReportTableCell(formatAmt(openingBalance), colWidth)
                        if (showTrIn) ReportTableCell(formatAmt(transferIn), colWidth, textColor = Color(0xFF1565C0))
                        if (showExp) ReportTableCell(formatAmt(expenditure), colWidth, textColor = Color(0xFFC62828))
                        if (showTrOut) ReportTableCell(formatAmt(transferOut), colWidth, textColor = Color(0xFFEF6C00))
                        if (showClBal) ReportTableCell(
                            formatAmt(closingBalance),
                            colWidth,
                            fontWeight = FontWeight.Bold,
                            textColor = if (closingBalance >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    // Expanded Transaction Log below this member row (Compact, Near, and Readable)
                    AnimatedVisibility(visible = isExpanded) {
                        val memberMeals = remember(allMeals, member.name, fromDateObj, toDateObj) {
                            allMeals.filter {
                                it.name == member.name && it.date.time >= fromDateObj.time && it.date.time <= toDateObj.time
                            }.sortedBy { it.date }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            MemberTransactionDetailsView(
                                memberName = member.name,
                                transactions = memberMeals
                            )
                        }
                    }
                }
            }

            // Combined Totals Footer Row
            if (reportDataList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .height(IntrinsicSize.Min)
                    ) {
                        ReportTableCell("TOTAL (Combined)", nameWidth, MaterialTheme.colorScheme.onPrimaryContainer, true, FontWeight.Bold)
                        if (showOpBal) ReportTableCell(formatAmt(totalOp), colWidth, MaterialTheme.colorScheme.onPrimaryContainer, true, FontWeight.Bold)
                        if (showTrIn) ReportTableCell(formatAmt(totalTrIn), colWidth, Color(0xFF1565C0), true, FontWeight.Bold)
                        if (showExp) ReportTableCell(formatAmt(totalExp), colWidth, Color(0xFFC62828), true, FontWeight.Bold)
                        if (showTrOut) ReportTableCell(formatAmt(totalTrOut), colWidth, Color(0xFFEF6C00), true, FontWeight.Bold)
                        if (showClBal) ReportTableCell(
                            formatAmt(totalCl),
                            colWidth,
                            textColor = if (totalCl >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            isHeader = true,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        com.kushal.mealapp.GlobalCalculatorOverlay()
    }
}

@Composable
fun VerticalCardView(
    reportDataList: List<MemberReportData>,
    allMeals: List<Meal1>,
    fromDateObj: Date,
    toDateObj: Date
) {
    var expandedMemberId by remember { mutableStateOf<Long?>(null) }

    val totalOp = remember(reportDataList) { reportDataList.sumOf { it.openingBalance } }
    val totalTrIn = remember(reportDataList) { reportDataList.sumOf { it.transferIn } }
    val totalExp = remember(reportDataList) { reportDataList.sumOf { it.expenditure } }
    val totalTrOut = remember(reportDataList) { reportDataList.sumOf { it.transferOut } }
    val totalCl = remember(reportDataList) { reportDataList.sumOf { it.closingBalance } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(reportDataList) { _, data ->
            val member = data.member
            val openingBalance = data.openingBalance
            val transferIn = data.transferIn
            val expenditure = data.expenditure
            val transferOut = data.transferOut
            val closingBalance = data.closingBalance

            val isExpanded = expandedMemberId == member.id

            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMemberId = if (isExpanded) null else member.id },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isExpanded) "▲ Hide" else "▼ View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (closingBalance >= 0.0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "Balance: ${formatAmt(closingBalance)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (closingBalance >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Op. Bal", formatAmt(openingBalance))
                        MetricItem("Transfer In", formatAmt(transferIn), Color(0xFF1565C0))
                        MetricItem("Expenditure", formatAmt(expenditure), Color(0xFFC62828))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Transfer Out", formatAmt(transferOut), Color(0xFFEF6C00))
                        MetricItem("Cl. Bal", formatAmt(closingBalance), fontWeight = FontWeight.Bold, textColor = if (closingBalance >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828))
                    }

                    // Expanded Transaction Log below card
                    AnimatedVisibility(visible = isExpanded) {
                        val memberMeals = remember(allMeals, member.name, fromDateObj, toDateObj) {
                            allMeals.filter {
                                it.name == member.name && it.date.time >= fromDateObj.time && it.date.time <= toDateObj.time
                            }.sortedBy { it.date }
                        }

                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            MemberTransactionDetailsView(
                                memberName = member.name,
                                transactions = memberMeals
                            )
                        }
                    }
                }
            }
        }

        // Combined Summary Card for Vertical View
        if (reportDataList.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊 (All Accounts)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (totalCl >= 0.0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = "Net Cl. Bal: ${formatAmt(totalCl)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (totalCl >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem("Total Op. Bal", formatAmt(totalOp), fontWeight = FontWeight.Bold)
                            MetricItem("Total Tr. In", formatAmt(totalTrIn), Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                            MetricItem("Total Exp.", formatAmt(totalExp), Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem("Total Tr. Out", formatAmt(totalTrOut), Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                            MetricItem("Net Closing Bal", formatAmt(totalCl), if (totalCl >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberTransactionDetailsView(
    memberName: String,
    transactions: List<Meal1>
) {
    val totalAmount = remember(transactions) { transactions.sumOf { it.price } }

    Card(
        modifier = Modifier
            .widthIn(max = 330.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = "📋 Transaction Details for $memberName",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            // Table Header Row (Matching SummaryTable.kt style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0F7FA))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("S.No. / Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Item / Remark", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("Amount (Rs.)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            }

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions found for $memberName in the selected date range.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            } else {
                transactions.forEachIndexed { index, meal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(formatDate(meal.date), fontSize = 11.sp, color = Color.DarkGray)
                            Text(meal.name, fontSize = 12.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(meal.item, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6A1B9A))
                            Text(meal.expenditure, fontSize = 11.sp, color = Color.Gray)
                            Text(formatAmt(meal.price), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                        }
                    }
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }

                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

                // Summary Total Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Amount:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(formatAmt(totalAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, textColor: Color = MaterialTheme.colorScheme.onSurface, fontWeight: FontWeight = FontWeight.Normal) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = fontWeight, color = textColor)
    }
}

@Composable
fun ReportTableCell(
    text: String,
    width: Dp,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isHeader: Boolean = false,
    fontWeight: FontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = fontWeight,
            fontSize = 13.sp
        )
    }
}

private fun formatAmt(amount: Double): String {
    return String.format(Locale.US, "%.2f", amount)
}