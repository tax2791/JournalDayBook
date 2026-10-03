@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@file:Suppress("UsingMaterialAndMaterial3Libraries")

package database

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.DriveBackupManager
import java.util.Calendar
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import com.kushal.mealapp.BannerAdView
import kotlin.math.abs

// --- Constants for Dropdown Menu ---
private const val ALL_OPTION = "All"
//SummyTable
@SuppressLint("DefaultLocale")
@Composable
fun SummaryTable(viewModel: MealViewModel) {
    val allMeals by viewModel.allMeals1.observeAsState(emptyList())
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())

    // --- Filter States (Default to Current Month) ---
    val currentLocalDate = remember { LocalDate.now() }
    val defaultFromDateStr = remember {
        val firstDay = currentLocalDate.withDayOfMonth(1)
        formatDate(firstDay.toDate())
    }
    val defaultToDateStr = remember {
        val lastDay = currentLocalDate.withDayOfMonth(currentLocalDate.lengthOfMonth())
        formatDate(lastDay.toDate())
    }

    var fromDate by remember { mutableStateOf(defaultFromDateStr) }
    var toDate by remember { mutableStateOf(defaultToDateStr) }
    var selectedName by remember { mutableStateOf(ALL_OPTION) } // Name filter
    var selectedItem by remember { mutableStateOf(ALL_OPTION) } // Item filter

    // Dialog states for Date Pickers
    val fromDateDialogState = rememberMaterialDialogState()
    val toDateDialogState = rememberMaterialDialogState()

    // --- Derived Data for Filters ---
    val uniqueMealNames = remember(allMembers) {
        (allMembers.map { it.name }.distinct() + ALL_OPTION).sorted()
    }
    val uniqueMealItems = remember(allMeals) {
        (allMeals.map { it.item }.distinct() + ALL_OPTION).sorted()
    }

    // --- Filter Logic ---
    val filteredMeals = remember(allMeals, fromDate, toDate, selectedName, selectedItem) {
        allMeals.filter { meal ->
            val mealDate = formatDate(meal.date)

            val isDateInRange = (fromDate.isEmpty() || mealDate >= fromDate) &&
                    (toDate.isEmpty() || mealDate <= toDate)

            val isNameMatch = selectedName == ALL_OPTION || meal.name == selectedName
            val isItemMatch = selectedItem == ALL_OPTION || meal.item == selectedItem

            isDateInRange && isNameMatch && isItemMatch
        }
    }

    // --- Computed Total Amount for Filtered Rows ---
    val totalFilteredAmount = remember(filteredMeals) {
        filteredMeals.sumOf { it.price }
    }

    // Dialog states for Edit / Delete actions on long press
    var selectedMealForAction by remember { mutableStateOf<Meal1?>(null) }
    var mealToEdit by remember { mutableStateOf<Meal1?>(null) }
    var mealToDelete by remember { mutableStateOf<Meal1?>(null) }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BannerAdView()
            }
        }
    ) { innerPadding ->
        // --- UI Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            // Header & Export Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Export PDF Button
                    Button(
                        onClick = {
                            if (filteredMeals.isEmpty()) {
                                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                            } else {
                                val pdfFile = MemberReportExporter.exportSummaryToPdf(
                                    context, filteredMeals, fromDate, toDate, selectedName, selectedItem
                                )
                                if (pdfFile != null) {
                                    Toast.makeText(context, "Summary PDF Exported ✅", Toast.LENGTH_SHORT).show()
                                    MemberReportExporter.shareOrOpenFile(
                                        context, pdfFile, "application/pdf", "Share Summary PDF"
                                    )
                                } else {
                                    Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Export Excel Button
                    Button(
                        onClick = {
                            if (filteredMeals.isEmpty()) {
                                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                            } else {
                                val csvFile = MemberReportExporter.exportSummaryToExcelCsv(
                                    context, filteredMeals, fromDate, toDate, selectedName, selectedItem
                                )
                                if (csvFile != null) {
                                    Toast.makeText(context, "Summary Excel CSV Exported ✅", Toast.LENGTH_SHORT).show()
                                    MemberReportExporter.shareOrOpenFile(
                                        context, csvFile, "text/csv", "Share Summary Excel CSV"
                                    )
                                } else {
                                    Toast.makeText(context, "Failed to export Excel CSV", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Excel", tint = Color.White, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Filter Section
            FilterControls(
                fromDate = fromDate,
                toDate = toDate,
                selectedName = selectedName,
                onNameSelected = { selectedName = it },
                uniqueNames = uniqueMealNames,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                uniqueItems = uniqueMealItems,
                fromDateDialogState = fromDateDialogState,
                toDateDialogState = toDateDialogState,
                onResetDates = {
                    fromDate = defaultFromDateStr
                    toDate = defaultToDateStr
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Table Start
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.LightGray)
            ) {
                // Table Header (Fixed)
                TableHeaderRow()

                // Scrollable Table Content
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    if (filteredMeals.isEmpty()) {
                        item {
                            Text(
                                "No meals found for the selected filters.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    itemsIndexed(
                        items = filteredMeals,
                        key = { _, meal -> meal.id }
                    ) { index, meal ->
                        MealRow(
                            index = index,
                            meal = meal,
                            onLongClick = { targetMeal ->
                                selectedMealForAction = targetMeal
                            }
                        )
                        if (index < filteredMeals.lastIndex) {
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
                }

                // Table Total Footer Row (Visible when list is active)
                if (filteredMeals.isNotEmpty()) {
                    Divider(color = Color.DarkGray, thickness = 1.dp)
                    TableTotalRow(totalFilteredAmount = totalFilteredAmount)
                }
            }
            // Table End

            // Date Picker Dialogs
            DateDialog(fromDateDialogState, "Select From Date") { selectedDate ->
                fromDate = formatDate(selectedDate.toDate())
            }

            DateDialog(toDateDialogState, "Select To Date") { selectedDate ->
                toDate = formatDate(selectedDate.toDate())
            }

            // Action Options Dialog (Long Press)
            selectedMealForAction?.let { meal ->
                TransactionActionMenuDialog(
                    meal = meal,
                    onDismiss = { selectedMealForAction = null },
                    onEdit = {
                        mealToEdit = meal
                        selectedMealForAction = null
                    },
                    onDelete = {
                        mealToDelete = meal
                        selectedMealForAction = null
                    }
                )
            }

            // Edit Dialog
            mealToEdit?.let { meal ->
                EditTransactionDialog(
                    meal = meal,
                    allMembers = allMembers,
                    allMeals1 = allMeals,
                    onDismiss = { mealToEdit = null },
                    onSave = { updatedMeal ->
                        viewModel.updateMeal1(updatedMeal)
                        Toast.makeText(context, "Transaction updated successfully ✅", Toast.LENGTH_SHORT).show()
                        DriveBackupManager.syncAutomatically(context)
                        mealToEdit = null
                    }
                )
            }

            // Delete Dialog
            mealToDelete?.let { meal ->
                DeleteTransactionDialog(
                    meal = meal,
                    onDismiss = { mealToDelete = null },
                    onConfirmDelete = {
                        viewModel.deleteMeal1(meal)
                        Toast.makeText(context, "Transaction deleted ✅", Toast.LENGTH_SHORT).show()
                        DriveBackupManager.syncAutomatically(context)
                        mealToDelete = null
                    }
                )
            }
        }
    }
}

// --- Styled Components ---

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0F7FA)) // Light Blue Background
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("S.No. / Date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text("Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text("Item / Remark", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text("Amount (Rs.)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
    }
}

@Composable
private fun MealRow(
    index: Int,
    meal: Meal1,
    onLongClick: (Meal1) -> Unit
) {
    val formattedDate = formatDate(meal.date)
    val formattedPrice = formatCurrency(meal.price)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(meal) }
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text((index + 1).toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(formattedDate, fontSize = 12.sp, color = Color.DarkGray)
            Text(meal.name, fontSize = 14.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(meal.item, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6A1B9A))
            Text(meal.expenditure, fontSize = 12.sp, color = Color.Gray)
            Text(formattedPrice, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
        }
    }
}

@Composable
private fun TableTotalRow(totalFilteredAmount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8F5E9)) // Soft green accent for total summary
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Total Amount:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = formatCurrency(totalFilteredAmount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF004D40)
        )
    }
}

@Composable
private fun FilterControls(
    fromDate: String,
    toDate: String,
    selectedName: String,
    onNameSelected: (String) -> Unit,
    uniqueNames: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    uniqueItems: List<String>,
    fromDateDialogState: MaterialDialogState,
    toDateDialogState: MaterialDialogState,
    onResetDates: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { fromDateDialogState.show() },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = if (fromDate.isEmpty()) "From Date" else "From: $fromDate",
                    color = Color.Black
                )
            }

            Button(
                onClick = { toDateDialogState.show() },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = if (toDate.isEmpty()) "To Date" else "To: $toDate",
                    color = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DropdownFilter(
                label = "Filter by Name",
                selectedValue = selectedName,
                options = uniqueNames,
                onValueSelected = onNameSelected,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )

            DropdownFilter(
                label = "Filter by Item",
                selectedValue = selectedItem,
                options = uniqueItems,
                onValueSelected = onItemSelected,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onResetDates()
                onNameSelected(ALL_OPTION)
                onItemSelected(ALL_OPTION)
            },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFEBEE))
        ) {
            Text("Reset Filters", color = Color.Black)
        }
    }
}

@Composable
fun DropdownFilter(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.clickable { expanded = true }) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onValueSelected(option)
                    expanded = false
                }) {
                    Text(text = option)
                }
            }
        }
    }
}

@Composable
fun DateDialog(
    dialogState: MaterialDialogState,
    title: String,
    onDateSelected: (LocalDate) -> Unit
) {
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now(),
            title = title
        ) { date ->
            onDateSelected(date)
        }
    }
}

// --- Helper Functions ---

fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

private val currencyFormatter = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue() = DecimalFormat("#,##0.00")
}

private val dateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
}

fun formatCurrency(value: Double): String = currencyFormatter.get()?.format(value) ?: value.toString()

fun formatDate(date: Date): String = dateFormatter.get()?.format(date) ?: date.toString()

// --- Dialogs for Long Press Options, Edit & Delete ---

@Composable
fun TransactionActionMenuDialog(
    meal: Meal1,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Transaction Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${meal.name} • ${meal.item} (₹${formatCurrency(meal.price)})",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("✏️ Edit Transaction", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                ) {
                    Text("🗑️ Delete Transaction", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditTransactionDialog(
    meal: Meal1,
    allMembers: List<Member>,
    allMeals1: List<Meal1>,
    onDismiss: () -> Unit,
    onSave: (Meal1) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    // Initial Mode Detection
    val initialMode = remember(meal) {
        when {
            meal.item.startsWith("Receipt", ignoreCase = true) || meal.item.equals("Deposit", ignoreCase = true) -> "Receipt"
            meal.item.equals("Transfer In", ignoreCase = true) || meal.item.equals("Transfer Out", ignoreCase = true) -> "Money Transfer"
            else -> "Regular Expenditure"
        }
    }

    var formMode by remember { mutableStateOf(initialMode) }
    var expandedFormMode by remember { mutableStateOf(false) }
    val formModes = listOf("Regular Expenditure", "Money Transfer", "Receipt")

    // Categories setup (identical to ExpForm.kt)
    val defaultCategories = listOf("Vegetable", "Grocery", "Admin", "Travel", "Education")
    val dynamicCategories by remember(allMeals1) {
        derivedStateOf {
            val customFromDb = allMeals1.map { it.item }
                .filter { it.isNotBlank() && !it.startsWith("Receipt") && it !in listOf("Transfer In", "Transfer Out", "Receipt", "Deposit") }
                .distinct()
            (defaultCategories + customFromDb + "Other").distinct()
        }
    }

    val defaultReceiptCategories = listOf("Salary", "Interest", "Bonus", "Dividend", "Refund", "Rental Income", "Business Income", "Gift")
    val dynamicReceiptCategories by remember(allMeals1) {
        derivedStateOf {
            val customFromDb = allMeals1.map { it.item }
                .filter { it.startsWith("Receipt") && it != "Receipt" }
                .map { it.removePrefix("Receipt: ").removePrefix("Receipt - ") }
                .filter { it.isNotBlank() }
                .distinct()
            (defaultReceiptCategories + customFromDb + "Other").distinct()
        }
    }

    // State Initialization
    var selectedName by remember { mutableStateOf(meal.name) }
    
    val isDefaultCat = meal.item in dynamicCategories && meal.item != "Other"
    var selectedItem by remember { mutableStateOf(if (isDefaultCat) meal.item else "Other") }
    var customCategory by remember { mutableStateOf(if (!isDefaultCat) meal.item else "") }

    val rawReceiptCat = meal.item.removePrefix("Receipt: ").removePrefix("Receipt - ")
    val isDefaultReceiptCat = rawReceiptCat in dynamicReceiptCategories && rawReceiptCat != "Other"
    var selectedReceiptCategory by remember { mutableStateOf(if (isDefaultReceiptCat) rawReceiptCat else "Other") }
    var customReceiptCategory by remember { mutableStateOf(if (!isDefaultReceiptCat) rawReceiptCat else "") }

    var expenditure by remember { mutableStateOf(meal.expenditure) }
    var priceText by remember { mutableStateOf(abs(meal.price).toString()) }

    var expandedName by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedReceiptCategory by remember { mutableStateOf(false) }

    var dateInput by remember { mutableStateOf(dateFormat.format(meal.date)) }
    var selectedDate by remember { mutableStateOf<Date?>(meal.date) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✏️ Edit Transaction",
                    style = MaterialTheme.typography.h6.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp,
                backgroundColor = Color(0xFFFDFDFD),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    // 1. Transaction Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedFormMode,
                        onExpandedChange = { expandedFormMode = !expandedFormMode }
                    ) {
                        OutlinedTextField(
                            value = formMode,
                            onValueChange = {},
                            label = { Text("Transaction Type") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFormMode,
                            onDismissRequest = { expandedFormMode = false }
                        ) {
                            formModes.forEach { mode ->
                                DropdownMenuItem(onClick = {
                                    formMode = mode
                                    expandedFormMode = false
                                }) {
                                    Text(mode)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Name Dropdown (Account / Member)
                    ExposedDropdownMenuBox(
                        expanded = expandedName,
                        onExpandedChange = { expandedName = !expandedName }
                    ) {
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            label = { Text(if (formMode == "Receipt") "Select Account / Member" else "Select Name") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedName,
                            onDismissRequest = { expandedName = false }
                        ) {
                            allMembers.forEach { member ->
                                DropdownMenuItem(onClick = {
                                    selectedName = member.name
                                    expandedName = false
                                }) {
                                    Text(member.name)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Category / Item Fields
                    if (formMode == "Regular Expenditure") {
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory }
                        ) {
                            OutlinedTextField(
                                value = selectedItem,
                                onValueChange = {},
                                label = { Text("Category") },
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                dynamicCategories.forEach { cat ->
                                    DropdownMenuItem(onClick = {
                                        selectedItem = cat
                                        expandedCategory = false
                                    }) {
                                        Text(cat)
                                    }
                                }
                            }
                        }

                        if (selectedItem == "Other") {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customCategory,
                                onValueChange = { customCategory = it },
                                label = { Text("Specify Custom Category") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else if (formMode == "Receipt") {
                        ExposedDropdownMenuBox(
                            expanded = expandedReceiptCategory,
                            onExpandedChange = { expandedReceiptCategory = !expandedReceiptCategory }
                        ) {
                            OutlinedTextField(
                                value = selectedReceiptCategory,
                                onValueChange = {},
                                label = { Text("Receipt Type / Category") },
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedReceiptCategory,
                                onDismissRequest = { expandedReceiptCategory = false }
                            ) {
                                dynamicReceiptCategories.forEach { cat ->
                                    DropdownMenuItem(onClick = {
                                        selectedReceiptCategory = cat
                                        expandedReceiptCategory = false
                                    }) {
                                        Text(cat)
                                    }
                                }
                            }
                        }

                        if (selectedReceiptCategory == "Other") {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customReceiptCategory,
                                onValueChange = { customReceiptCategory = it },
                                label = { Text("Specify Custom Receipt Type") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Money Transfer
                        OutlinedTextField(
                            value = selectedItem,
                            onValueChange = { selectedItem = it },
                            label = { Text("Transfer Type (e.g. Transfer In / Transfer Out)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Description / Remark
                    OutlinedTextField(
                        value = expenditure,
                        onValueChange = { expenditure = it },
                        label = { Text("Remark / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Amount (₹)
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 6. Select Date Field
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = {},
                        label = { Text("Select Date") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                modifier = Modifier.clickable {
                                    val calendar = Calendar.getInstance().apply {
                                        if (selectedDate != null) time = selectedDate
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selected = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }.time
                                            selectedDate = selected
                                            dateInput = dateFormat.format(selected)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceText.toDoubleOrNull() ?: meal.price
                    val finalPrice = if (meal.item == "Transfer Out") -abs(parsedPrice) else abs(parsedPrice)

                    val finalItem = when (formMode) {
                        "Regular Expenditure" -> if (selectedItem == "Other") customCategory.trim() else selectedItem
                        "Receipt" -> {
                            val cat = if (selectedReceiptCategory == "Other") customReceiptCategory.trim() else selectedReceiptCategory
                            if (cat.isNotBlank()) "Receipt: $cat" else "Receipt"
                        }
                        else -> selectedItem.ifBlank { "Transfer In" }
                    }

                    val updatedMeal = meal.copy(
                        name = selectedName.trim(),
                        item = finalItem,
                        expenditure = expenditure.trim(),
                        price = finalPrice,
                        date = selectedDate ?: meal.date
                    )
                    onSave(updatedMeal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = selectedName.isNotBlank() && priceText.isNotBlank() && selectedDate != null
            ) {
                Text("Save Changes", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteTransactionDialog(
    meal: Meal1,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🗑️ Delete Transaction",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Red
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete this transaction for \"${meal.name}\" (${meal.item} - ₹${formatCurrency(meal.price)})?\n\nThis action cannot be undone.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}