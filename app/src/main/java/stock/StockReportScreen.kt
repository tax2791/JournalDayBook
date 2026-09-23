package stock

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun StockReportScreen(viewModel: StockViewModel = viewModel()) {
    val stockList by viewModel.stockList.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("Day") }

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormatter.format(Date())
    var fromDate by remember { mutableStateOf(today) }
    var toDate by remember { mutableStateOf(today) }

    var selectedMonth by remember { mutableStateOf(SimpleDateFormat("yyyy-MM").format(Date())) }
    var selectedYear by remember { mutableStateOf(SimpleDateFormat("yyyy").format(Date())) }

    // ✅ Date Pickers
    val cal = Calendar.getInstance()
    val fromPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            fromDate = "%04d-%02d-%02d".format(y, m + 1, d)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    val toPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            toDate = "%04d-%02d-%02d".format(y, m + 1, d)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    // ✅ Filtering logic
    val filteredList = remember(stockList, selectedFilter, fromDate, toDate, selectedMonth, selectedYear) {
        when (selectedFilter) {
            "Day" -> stockList.filter { it.date in fromDate..toDate }
            "Month" -> stockList.filter { it.date.startsWith(selectedMonth) }
            "Year" -> stockList.filter { it.date.startsWith(selectedYear) }
            else -> stockList
        }
    }

    // ✅ Calculations
    val totalOpening = filteredList.filter { it.stock_type.equals("Opening", true) }
        .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
    val totalReceived = filteredList.filter { it.stock_type.equals("Receipt", true) }
        .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
    val totalIssued = filteredList.filter { it.stock_type.equals("Issue", true) }
        .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
//    val profitLoss = filteredList.sumOf {
//        when (it.stock_type.lowercase()) {
//            "issue" -> -(it.total_value.toDoubleOrNull() ?: 0.0)
//            else -> (it.total_value.toDoubleOrNull() ?: 0.0)
//        }
//    }
    val balanceQty = totalOpening + totalReceived - totalIssued

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock Report") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Filter Selector
            item {
                FilterDropdown(selectedFilter) { selectedFilter = it }
                Spacer(Modifier.height(12.dp))

                when (selectedFilter) {
                    "Day" -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = { fromPicker.show() }) {
                                Text("From: $fromDate")
                            }
                            Button(onClick = { toPicker.show() }) {
                                Text("To: $toDate")
                            }
                        }
                    }

                    "Month" -> {
                        MonthPicker(selectedMonth) { selectedMonth = it }
                    }

                    "Year" -> {
                        YearPicker(selectedYear) { selectedYear = it }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 🔹 Summary
            item {
                SummaryCard("Total Opening Quantity", totalOpening)
                SummaryCard("Total Received Quantity", totalReceived)
                SummaryCard("Total Issued Quantity", totalIssued)
                SummaryCard("Balance Quantity", balanceQty)
               // SummaryCard("Profit / Loss (Value)", profitLoss)
                Spacer(Modifier.height(16.dp))
            }

            // 🔹 Bar Chart
            item {
                Text("Bar Chart (Received vs Issued)", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                SimpleBarChart(totalReceived, totalIssued)
                Spacer(Modifier.height(24.dp))
            }

            // 🔹 Pie Chart
            item {
                Text("Stock Distribution Pie Chart", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                SimplePieChart(totalReceived, totalIssued, balanceQty)
                Spacer(Modifier.height(24.dp))
            }

            // 🔹 Item-wise report
            item {
                Text("Item-wise Report", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            val grouped = filteredList.groupBy { it.item_name }
            grouped.forEach { (itemName, list) ->
                val openingQty = list.filter { it.stock_type.equals("Opening", true) }
                    .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                val openingValue = list.filter { it.stock_type.equals("Opening", true) }
                    .sumOf { it.total_value.toDoubleOrNull() ?: 0.0 }

                val receivedQty = list.filter { it.stock_type.equals("Receipt", true) }
                    .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                val receivedValue = list.filter { it.stock_type.equals("Receipt", true) }
                    .sumOf { it.total_value.toDoubleOrNull() ?: 0.0 }

                val issuedQty = list.filter { it.stock_type.equals("Issue", true) }
                    .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }

                // 🔹 Calculate Average Cost per unit
                val totalQtyBeforeIssue = openingQty + receivedQty
                val totalValueBeforeIssue = openingValue + receivedValue
                val avgCostPerUnit = if (totalQtyBeforeIssue > 0)
                    totalValueBeforeIssue / totalQtyBeforeIssue else 0.0

                // 🔹 Calculate Issued Value (at average cost)
                val issuedValue = issuedQty * avgCostPerUnit

                // 🔹 Final total value (remaining stock value)
                val totalValue = totalValueBeforeIssue - issuedValue
                val balanceQty = openingQty + receivedQty - issuedQty

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(itemName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Opening Qty: %.2f".format(openingQty))
                            Text("Received Qty: %.2f".format(receivedQty))
                            Text("Issued Qty: %.2f".format(issuedQty))
                            Text("Balance Qty: %.2f".format(balanceQty), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Avg Cost/Unit: %.2f".format(avgCostPerUnit))
                            Text("Total Value (after issues): %.2f".format(totalValue), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun FilterDropdown(selected: String, onSelectedChange: (String) -> Unit) {
    val options = listOf("Day", "Month", "Year")
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text("Filter: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelectedChange(option)
                    }
                )
            }
        }
    }
}

@Composable
fun MonthPicker(selected: String, onSelect: (String) -> Unit) {
    val months = (1..12).map { "%02d".format(it) }
    val years = (2020..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    var selYear by remember { mutableStateOf(selected.take(4)) }
    var selMonth by remember { mutableStateOf(selected.takeLast(2)) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expandedMonth = true }) { Text("Month: $selMonth") }
        Button(onClick = { expandedYear = true }) { Text("Year: $selYear") }
    }

    DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
        months.forEach {
            DropdownMenuItem(text = { Text(it) }, onClick = {
                selMonth = it
                expandedMonth = false
                onSelect("$selYear-$selMonth")
            })
        }
    }
    DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
        years.forEach {
            DropdownMenuItem(text = { Text(it) }, onClick = {
                selYear = it
                expandedYear = false
                onSelect("$selYear-$selMonth")
            })
        }
    }
}

@Composable
fun YearPicker(selected: String, onSelect: (String) -> Unit) {
    val years = (2020..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text("Year: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = {
                    expanded = false
                    onSelect(it)
                })
            }
        }
    }
}

@Composable
fun SimpleBarChart(received: Double, issued: Double) {
    val maxVal = maxOf(received, issued, 1.0)
    val barWidth = 80.dp
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .height((150 * (received / maxVal)).dp)
                    .width(barWidth)
                    .background(Color(0xFF4CAF50))
            )
            Text("Received")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .height((150 * (issued / maxVal)).dp)
                    .width(barWidth)
                    .background(Color(0xFFF44336))
            )
            Text("Issued")
        }
    }
}

@Composable
fun SimplePieChart(received: Double, issued: Double, balance: Double) {
    val total = received + issued + balance
    if (total == 0.0) {
        Text("No data to show")
        return
    }
    val proportions = listOf(received / total, issued / total, balance / total)
    val colors = listOf(Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF2196F3))

    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f
        proportions.forEachIndexed { index, proportion ->
            val sweep = (proportion * 360).toFloat()
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweep
        }
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        listOf("Received", "Issued", "Balance").forEachIndexed { i, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(colors[i])
                )
                Spacer(Modifier.width(4.dp))
                Text(label)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
