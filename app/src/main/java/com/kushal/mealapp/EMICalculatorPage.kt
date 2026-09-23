package com.kushal.mealapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.pow

// --- Models ---

data class EMIMonthly(
    val month: Int,
    val principal: Double,
    val interest: Double,
    val balance: Double
)

data class EMIResultData(
    val emi: Double,
    val totalPrincipal: Double,
    val totalInterest: Double,
    val totalPayment: Double,
    val breakdown: List<EMIMonthly>
)

enum class ConverterType(val title: String) {
    EMI("EMI"),
    FOREX("Forex (FX)"),
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature")
}

// --- Main Container Screen ---

@Composable
fun EMICalculatorPage(onBack: () -> Unit) {
    BackHandler { onBack() }
    var currentTab by remember { mutableStateOf(ConverterType.EMI) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Converter Hub", fontWeight = FontWeight.Bold) },
                backgroundColor = Color(0xFF1565C0),
                contentColor = Color.White,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                elevation = 0.dp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFFE3F2FD), Color(0xFFF8FAFC))
                    )
                )
        ) {
            // Scrollable Tab Row for all converter categories
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                backgroundColor = Color(0xFF1565C0),
                contentColor = Color.White,
                edgePadding = 12.dp
            ) {
                ConverterType.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "ConverterSwitcher"
            ) { activeTab ->
                when (activeTab) {
                    ConverterType.EMI -> EMICalculatorContent()
                    ConverterType.FOREX -> LiveForexConverterContent()
                    ConverterType.LENGTH -> UnitConverterContent(
                        categoryName = "Length Converter",
                        units = listOf("Meters", "Kilometers", "Centimeters", "Millimeters", "Miles", "Yards", "Feet", "Inches"),
                        defaultFrom = "Meters",
                        defaultTo = "Feet",
                        conversionLogic = ::convertLength
                    )
                    ConverterType.WEIGHT -> UnitConverterContent(
                        categoryName = "Weight & Mass Converter",
                        units = listOf("Kilograms", "Grams", "Milligrams", "Pounds (lbs)", "Ounces (oz)", "Metric Tons"),
                        defaultFrom = "Kilograms",
                        defaultTo = "Pounds (lbs)",
                        conversionLogic = ::convertWeight
                    )
                    ConverterType.TEMPERATURE -> UnitConverterContent(
                        categoryName = "Temperature Converter",
                        units = listOf("Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"),
                        defaultFrom = "Celsius (°C)",
                        defaultTo = "Fahrenheit (°F)",
                        conversionLogic = ::convertTemperature
                    )
                }
            }
        }
    }
}

// --- Tab 1: Animated EMI Screen ---

@Composable
fun EMICalculatorContent() {
    var principal by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var tenureMonths by remember { mutableStateOf("") }
    var resultData by remember { mutableStateOf<EMIResultData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = 4.dp,
                backgroundColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Loan Parameters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = principal,
                        onValueChange = { principal = it; errorMessage = null },
                        label = { Text("Principal Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = interest,
                        onValueChange = { interest = it; errorMessage = null },
                        label = { Text("Annual Interest Rate (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tenureMonths,
                        onValueChange = { tenureMonths = it; errorMessage = null },
                        label = { Text("Loan Tenure (Months)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val computed = computeEMI(principal, interest, tenureMonths)
                            if (computed != null) {
                                resultData = computed
                                errorMessage = null
                            } else {
                                errorMessage = "Please enter valid positive numbers."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0))
                    ) {
                        Text("Calculate EMI", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage!!, color = MaterialTheme.colors.error, fontSize = 13.sp)
                    }
                }
            }
        }

        resultData?.let { data ->
            item {
                val animatedEMI by animateFloatAsState(
                    targetValue = data.emi.toFloat(),
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "EMIAnimation"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 6.dp,
                    backgroundColor = Color(0xFF0D47A1)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Monthly Installment", color = Color(0xFF90CAF9), fontSize = 13.sp)
                        Text(
                            text = "₹${"%.2f".format(animatedEMI)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        MetricRow("Total Principal", "₹${"%.2f".format(data.totalPrincipal)}")
                        MetricRow("Total Interest", "₹${"%.2f".format(data.totalInterest)}")
                        MetricRow("Total Repayment", "₹${"%.2f".format(data.totalPayment)}")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color(0xFF1976D2),
                    elevation = 2.dp
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Month", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Principal", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Interest", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Balance", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            itemsIndexed(data.breakdown) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 1.dp,
                    backgroundColor = if (index % 2 == 0) Color(0xFFF1F8FE) else Color.White
                ) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.month}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text("₹${"%.0f".format(item.principal)}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text("₹${"%.0f".format(item.interest)}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text("₹${"%.0f".format(item.balance)}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- Tab 2: Live Forex Converter ---

@Composable
fun LiveForexConverterContent() {
    var amount by remember { mutableStateOf("100") }
    var baseCurrency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf("INR") }
    var convertedResult by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val currencyList = listOf("USD", "INR", "EUR", "GBP", "AED", "CAD", "JPY", "AUD", "SGD", "CNY")

    fun convert() {
        val parsed = amount.toDoubleOrNull()
        if (parsed == null) {
            errorText = "Enter a valid amount"
            return
        }
        isLoading = true
        errorText = null

        scope.launch {
            try {
                val rate = fetchLiveRate(baseCurrency, targetCurrency)
                convertedResult = parsed * rate
            } catch (e: Exception) {
                errorText = "Could not fetch rates. Check your internet connection."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(baseCurrency, targetCurrency) {
        convert()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp,
            backgroundColor = Color.White
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Live Exchange Rates", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectDropdown(label = "From", selected = baseCurrency, options = currencyList) { baseCurrency = it }
                    IconButton(
                        onClick = {
                            val temp = baseCurrency
                            baseCurrency = targetCurrency
                            targetCurrency = temp
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Swap", tint = Color(0xFF1565C0))
                    }
                    SelectDropdown(label = "To", selected = targetCurrency, options = currencyList) { targetCurrency = it }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { convert() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Convert", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorText!!, color = MaterialTheme.colors.error, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (convertedResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFFE8F5E9),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Conversion Result", fontSize = 13.sp, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$amount $baseCurrency = ${"%.4f".format(convertedResult)} $targetCurrency",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}

// --- Universal Reusable Unit Converter Component (Length, Weight, Temp) ---

@Composable
fun UnitConverterContent(
    categoryName: String,
    units: List<String>,
    defaultFrom: String,
    defaultTo: String,
    conversionLogic: (Double, String, String) -> Double
) {
    var inputValue by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf(defaultFrom) }
    var toUnit by remember { mutableStateOf(defaultTo) }

    val parsed = inputValue.toDoubleOrNull()
    val output = if (parsed != null) conversionLogic(parsed, fromUnit, toUnit) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp,
            backgroundColor = Color.White
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(categoryName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Value to Convert") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectDropdown(label = "From", selected = fromUnit, options = units) { fromUnit = it }
                    IconButton(
                        onClick = {
                            val temp = fromUnit
                            fromUnit = toUnit
                            toUnit = temp
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Swap", tint = Color(0xFF1565C0))
                    }
                    SelectDropdown(label = "To", selected = toUnit, options = units) { toUnit = it }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (output != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFFE8F5E9),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Result", fontSize = 13.sp, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$inputValue $fromUnit =\n${"%.4f".format(output)} $toUnit",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}

// --- Dropdown Picker Component ---

@Composable
fun SelectDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("$label: $selected", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D47A1))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSelect(option)
                    expanded = false
                }) {
                    Text(option, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// --- Conversion Mathematics & Networking ---

fun computeEMI(principal: String, interest: String, tenureMonths: String): EMIResultData? {
    return try {
        val p = principal.toDouble()
        val annualRate = interest.toDouble()
        val n = tenureMonths.toInt()
        if (p <= 0 || annualRate <= 0 || n <= 0) return null

        val r = annualRate / (12 * 100)
        val emi = p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)

        val breakdown = mutableListOf<EMIMonthly>()
        var remainingPrincipal = p

        for (month in 1..n) {
            val interestPayment = remainingPrincipal * r
            val principalPayment = emi - interestPayment
            remainingPrincipal -= principalPayment
            if (remainingPrincipal < 0) remainingPrincipal = 0.0
            breakdown.add(EMIMonthly(month, principalPayment, interestPayment, remainingPrincipal))
        }

        val totalPrincipal = breakdown.sumOf { it.principal }
        val totalInterest = breakdown.sumOf { it.interest }
        EMIResultData(emi, totalPrincipal, totalInterest, totalPrincipal + totalInterest, breakdown)
    } catch (e: Exception) {
        null
    }
}

suspend fun fetchLiveRate(base: String, target: String): Double {
    return withContext(Dispatchers.IO) {
        // Free open endpoint with no authentication token needed
        val url = URL("https://open.er-api.com/v6/latest/$base")
        val response = url.readText()
        val json = JSONObject(response)
        val rates = json.getJSONObject("rates")
        rates.getDouble(target)
    }
}

fun convertLength(value: Double, from: String, to: String): Double {
    val toMeters = when (from) {
        "Meters" -> value
        "Kilometers" -> value * 1000.0
        "Centimeters" -> value / 100.0
        "Millimeters" -> value / 1000.0
        "Miles" -> value * 1609.344
        "Yards" -> value * 0.9144
        "Feet" -> value * 0.3048
        "Inches" -> value * 0.0254
        else -> value
    }
    return when (to) {
        "Meters" -> toMeters
        "Kilometers" -> toMeters / 1000.0
        "Centimeters" -> toMeters * 100.0
        "Millimeters" -> toMeters * 1000.0
        "Miles" -> toMeters / 1609.344
        "Yards" -> toMeters / 0.9144
        "Feet" -> toMeters / 0.3048
        "Inches" -> toMeters / 0.0254
        else -> toMeters
    }
}

fun convertWeight(value: Double, from: String, to: String): Double {
    val toGrams = when (from) {
        "Kilograms" -> value * 1000.0
        "Grams" -> value
        "Milligrams" -> value / 1000.0
        "Pounds (lbs)" -> value * 453.59237
        "Ounces (oz)" -> value * 28.349523
        "Metric Tons" -> value * 1000000.0
        else -> value
    }
    return when (to) {
        "Kilograms" -> toGrams / 1000.0
        "Grams" -> toGrams
        "Milligrams" -> toGrams * 1000.0
        "Pounds (lbs)" -> toGrams / 453.59237
        "Ounces (oz)" -> toGrams / 28.349523
        "Metric Tons" -> toGrams / 1000000.0
        else -> toGrams
    }
}

fun convertTemperature(value: Double, from: String, to: String): Double {
    val toCelsius = when (from) {
        "Celsius (°C)" -> value
        "Fahrenheit (°F)" -> (value - 32) * 5.0 / 9.0
        "Kelvin (K)" -> value - 273.15
        else -> value
    }
    return when (to) {
        "Celsius (°C)" -> toCelsius
        "Fahrenheit (°F)" -> (toCelsius * 9.0 / 5.0) + 32
        "Kelvin (K)" -> toCelsius + 273.15
        else -> toCelsius
    }
}