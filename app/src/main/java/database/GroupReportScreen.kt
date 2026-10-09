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

@Composable
fun GroupReportScreen(viewModel: MealViewModel) {
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

    // Active members filtered specifically for Group Members
    val activeMembers = remember(allMembers, fromDateObj, toDateObj) {
        allMembers.filter { member ->
            val jDate = member.joinDate
            val eDate = member.exitDate
            val isDateActive = (jDate == null || jDate.time <= toDateObj.time) && (eDate == null || eDate.time >= fromDateObj.time)
            isDateActive && member.type == "Group Member"
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
                    text = "👥 Group Report",
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

                    // View Format Selection (Table, Vertical Cards)
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
                AnimatedContent(targetState = selectedFormat, label = "GroupViewFormatAnimation") { format ->
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
