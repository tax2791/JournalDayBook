@file:OptIn(ExperimentalFoundationApi::class)
@file:Suppress("UsingMaterialAndMaterial3Libraries")

package database

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// --- Constants for Dropdown Menu ---
private const val ALL_OPTION = "All"

@SuppressLint("DefaultLocale")
@Composable
fun SummaryTable(viewModel: MealViewModel) {
    val allMeals by viewModel.allMeals1.observeAsState(emptyList())
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())

    // --- Filter States ---
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf(ALL_OPTION) } // New Name filter
    var selectedItem by remember { mutableStateOf(ALL_OPTION) } // New Item filter

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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        // --- UI Layout ---
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Meal Summary",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Filter Section
            FilterControls(
                fromDate = fromDate,
                onFromDateChange = { fromDate = it },
                toDate = toDate,
                onToDateChange = { toDate = it },
                selectedName = selectedName,
                onNameSelected = { selectedName = it },
                uniqueNames = uniqueMealNames,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                uniqueItems = uniqueMealItems,
                fromDateDialogState = fromDateDialogState,
                toDateDialogState = toDateDialogState
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Table Start
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                        MealRow(index, meal)
                        if (index < filteredMeals.lastIndex) {
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
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
        // First Column Group
        Column(modifier = Modifier.weight(1f)) {
            Text("S.No. / Date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text("Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Second Column Group (aligned to end)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text("Item / Remark", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text("Amount (Rs.)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
    }
}

@Composable
private fun MealRow(index: Int, meal: Meal1) {
    val formattedDate = formatDate(meal.date)
    val formattedPrice = formatCurrency(meal.price)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First Column Group
        Column(modifier = Modifier.weight(1f)) {
            Text((index + 1).toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black) // S.No.
            Text(formattedDate, fontSize = 12.sp, color = Color.DarkGray) // Date
            Text(meal.name, fontSize = 14.sp, color = Color.Black) // Name
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Second Column Group (aligned to end)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(meal.item, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6A1B9A)) // Item
            Text(meal.expenditure, fontSize = 12.sp, color = Color.Gray) // Remark
            Text(formattedPrice, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40)) // Amount
        }
    }
}


@Composable
private fun FilterControls(
    fromDate: String,
    onFromDateChange: (String) -> Unit,
    toDate: String,
    onToDateChange: (String) -> Unit,
    selectedName: String,
    onNameSelected: (String) -> Unit,
    uniqueNames: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    uniqueItems: List<String>,
    fromDateDialogState: MaterialDialogState,
    toDateDialogState: MaterialDialogState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Date Filters (From/To) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // From Date Picker
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

            // To Date Picker
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

        // --- Name and Item Filters (Dropdowns) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Name Filter
            DropdownFilter(
                label = "Filter by Name",
                selectedValue = selectedName,
                options = uniqueNames,
                onValueSelected = onNameSelected,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )

            // Item Filter
            DropdownFilter(
                label = "Filter by Item",
                selectedValue = selectedItem,
                options = uniqueItems,
                onValueSelected = onItemSelected,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Reset Button
        Button(
            onClick = {
                onFromDateChange("")
                onToDateChange("")
                onNameSelected(ALL_OPTION)
                onItemSelected(ALL_OPTION)
            },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFEBEE)) // Light Red
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

// --- Helper Functions (From original code) ---

fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

private val currencyFormatter = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue() = DecimalFormat("#,##0.00")
}

private val dateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
}

fun formatCurrency(value: Double): String = currencyFormatter.get()?.format(value) ?: value.toString()

fun formatDate(date: Date): String = dateFormatter.get()?.format(date) ?: date.toString()