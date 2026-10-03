package database

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kushal.mealapp.BannerAdView
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPieChart(viewModel: MealViewModel = viewModel()) {
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    val calendar = remember { Calendar.getInstance() }
    val currentMonthInt = remember { calendar.get(Calendar.MONTH) + 1 }
    val currentYearInt = remember { calendar.get(Calendar.YEAR) }

    val months = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
    val years = remember { (2020..2030).map { it.toString() } }
    val transactionTypeOptions = remember {
        listOf("All", "Expenditure", "Transfer In / Receipt", "Transfer Out")
    }

    var selectedMonth by remember { mutableStateOf(months[currentMonthInt - 1]) }
    var selectedYear by remember { mutableStateOf(currentYearInt.toString()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedType by remember { mutableStateOf("All") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Filter Logic
    val filteredMeals = remember(allMeals1, selectedMonth, selectedYear, selectedDate, selectedType) {
        val monthIdx = months.indexOf(selectedMonth) + 1
        val yrInt = selectedYear.toIntOrNull() ?: currentYearInt

        allMeals1.filter { meal ->
            val cal = Calendar.getInstance().apply { time = meal.date }
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            val d = Instant.ofEpochMilli(meal.date.time)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val dateMatch = if (selectedDate != null) {
                d == selectedDate
            } else {
                m == monthIdx && y == yrInt
            }

            val typeMatch = when (selectedType) {
                "Expenditure" -> !meal.item.startsWith("Receipt") && meal.item !in listOf("Deposit", "Transfer In", "Transfer Out")
                "Transfer In / Receipt" -> meal.item.startsWith("Receipt") || meal.item in listOf("Deposit", "Transfer In")
                "Transfer Out" -> meal.item == "Transfer Out" || meal.price < 0
                else -> true
            }

            dateMatch && typeMatch
        }
    }

    // Group filtered meals by item and sum absolute prices for chart visualization
    val itemPriceMap = remember(filteredMeals) {
        filteredMeals.groupBy { it.item }
            .mapValues { entry -> entry.value.sumOf { abs(it.price) } }
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

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = Color(0xFFF4F6F9),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Expense & Income Pie Chart",
                            color = Color(0xFF002B49),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${selectedMonth} ${selectedYear} (${selectedType})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filters Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Chart Filters",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF002B49),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Scrollable Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Dropdown
                        var expandedMonth by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedMonth = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Month: $selectedMonth", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                            DropdownMenu(
                                expanded = expandedMonth,
                                onDismissRequest = { expandedMonth = false }
                            ) {
                                months.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            selectedMonth = m
                                            selectedDate = null
                                            expandedMonth = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown
                        var expandedYear by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedYear = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Year: $selectedYear", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                            DropdownMenu(
                                expanded = expandedYear,
                                onDismissRequest = { expandedYear = false }
                            ) {
                                years.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y) },
                                        onClick = {
                                            selectedYear = y
                                            selectedDate = null
                                            expandedYear = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date Picker Button
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

                        // Transaction Type Dropdown
                        var expandedType by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedType = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Type: $selectedType", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                            DropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false }
                            ) {
                                transactionTypeOptions.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            selectedType = t
                                            expandedType = false
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
                }
            }

            // Interactive Chart Card
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
                    if (chartData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions found for the selected filters.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        InteractivePieChart(
                            data = chartData,
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractivePieChart(data: List<Pair<String, Double>>, colors: List<Color>) {
    val totalExpense = remember(data) { data.sumOf { it.second } }
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
                        .pointerInput(data, totalExpense) {
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
                                    data.forEachIndexed { index, (_, value) ->
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
                    data.forEachIndexed { index, (_, value) ->
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
                    .heightIn(max = 170.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                data.forEachIndexed { index, (item, value) ->
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
            val (selectedItem, selectedValue) = data[index]
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
                                text = "Total: ₹${"%.2f".format(selectedValue)}",
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
