@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package database

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.BannerAdView
import com.kushal.mealapp.DriveBackupManager
import com.kushal.mealapp.FloatingCalculatorState
import com.kushal.mealapp.GlobalCalculatorOverlay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpForm(
    viewModel: MealViewModel,
    onNavigateToAddMember: () -> Unit = {}
) {
    val membersState by viewModel.allMembers.collectAsState(initial = null)
    val members = membersState ?: emptyList()
    val isMembersLoaded = membersState != null
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    var showNoMemberDialog by remember { mutableStateOf(false) }

    LaunchedEffect(membersState) {
        if (membersState != null && membersState!!.isEmpty()) {
            showNoMemberDialog = true
        }
    }

    // Form Mode: "Regular Expenditure", "Money Transfer", or "Receipt"
    var formMode by remember { mutableStateOf("Regular Expenditure") }
    val formModes = listOf("Regular Expenditure", "Money Transfer", "Receipt")

    // --- Regular Expenditure States ---
    var selectedName by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var expenditure by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var meal by remember { mutableIntStateOf(0) }

    // --- Money Transfer States ---
    var fromAccount by remember { mutableStateOf("") }
    var toAccount by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var transferRemark by remember { mutableStateOf("") }

    // --- Receipt States ---
    var selectedReceiptCategory by remember { mutableStateOf("") }
    var customReceiptCategory by remember { mutableStateOf("") }
    var expandedReceiptCategory by remember { mutableStateOf(false) }

    // Dropdown expansion states
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedName by remember { mutableStateOf(false) }
    var expandedFromAccount by remember { mutableStateOf(false) }
    var expandedToAccount by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    // Common Date States (Default: Today's Date)
    var date by remember { mutableStateOf<Date?>(Date()) }
    var dateInput by remember { mutableStateOf(dateFormat.format(Date())) }

    fun stepDate(offsetDays: Int) {
        val cal = Calendar.getInstance()
        (date ?: Date()).let { cal.time = it }
        cal.add(Calendar.DAY_OF_MONTH, offsetDays)
        val selected = cal.time
        date = selected
        dateInput = dateFormat.format(selected)
    }

    fun setTodayDate() {
        val selected = Date()
        date = selected
        dateInput = dateFormat.format(selected)
    }

    // Default expenditure categories plus any custom categories
    val defaultCategories = listOf("Vegetable", "Grocery", "Admin", "Travel", "Education")
    val dynamicCategories by remember(allMeals1) {
        derivedStateOf {
            val customFromDb = allMeals1.map { it.item }
                .filter { it.isNotBlank() && !it.startsWith("Receipt") && it !in listOf("Transfer In", "Transfer Out", "Receipt", "Deposit") }
                .distinct()
            (defaultCategories + customFromDb + "Other").distinct()
        }
    }

    // Default receipt categories plus any custom receipt types
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

    val scrollState = rememberScrollState()

    val accentColor = when (formMode) {
        "Regular Expenditure" -> Color(0xFFF57C00)
        "Money Transfer" -> Color(0xFF1565C0)
        else -> Color(0xFF2E7D32)
    }

    val headerBgColor = when (formMode) {
        "Regular Expenditure" -> Color(0xFFFFF3E0)
        "Money Transfer" -> Color(0xFFE3F2FD)
        else -> Color(0xFFE8F5E9)
    }

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
                            "Transaction Entry Form",
                            color = Color(0xFF002B49),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Record Expenses, Transfers, or Income",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { com.kushal.mealapp.FloatingCalculatorState.show() }) {
                        Text("🧮", fontSize = 20.sp)
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
            // Mode Selector Pill Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    formModes.forEach { mode ->
                        val isSelected = formMode == mode
                        val modeColor = when (mode) {
                            "Regular Expenditure" -> Color(0xFFF57C00)
                            "Money Transfer" -> Color(0xFF1565C0)
                            else -> Color(0xFF2E7D32)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) modeColor else Color.Transparent)
                                .clickable { formMode = mode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (mode) {
                                    "Regular Expenditure" -> "💸 Expense"
                                    "Money Transfer" -> "🔄 Transfer"
                                    else -> "💰 Receipt"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else Color(0xFF555555),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Form Main Card Container
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Accent Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBgColor)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (formMode) {
                                        "Regular Expenditure" -> "💸"
                                        "Money Transfer" -> "🔄"
                                        else -> "💰"
                                    },
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (formMode) {
                                    "Regular Expenditure" -> "Add Regular Expenditure"
                                    "Money Transfer" -> "Inter-Account Money Transfer"
                                    else -> "Add Receipt / Income Entry"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Form Fields Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // If no member or account found in EntityTable, show redirect warning card
                        if (isMembersLoaded && members.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToAddMember() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GroupAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "No Members or Accounts Found",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "Tap here to add a member or personal account in MemberForm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = onNavigateToAddMember,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Add Now", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        // ==================== REGULAR EXPENDITURE FIELDS ====================
                        if (formMode == "Regular Expenditure") {
                            // NAME DROPDOWN
                            ExposedDropdownMenuBox(
                                expanded = expandedName,
                                onExpandedChange = { expandedName = !expandedName }
                            ) {
                                OutlinedTextField(
                                    value = selectedName,
                                    onValueChange = {},
                                    label = { Text("Select Name / Account") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedName) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedName,
                                    onDismissRequest = { expandedName = false }
                                ) {
                                    members.forEach { member ->
                                        DropdownMenuItem(
                                            text = { Text(member.name) },
                                            onClick = {
                                                selectedName = member.name
                                                expandedName = false
                                            }
                                        )
                                    }
                                }
                            }

                            // CATEGORY DROPDOWN
                            ExposedDropdownMenuBox(
                                expanded = expandedCategory,
                                onExpandedChange = { expandedCategory = !expandedCategory }
                            ) {
                                OutlinedTextField(
                                    value = selectedItem,
                                    onValueChange = {},
                                    label = { Text("Category") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCategory,
                                    onDismissRequest = { expandedCategory = false }
                                ) {
                                    dynamicCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                selectedItem = cat
                                                expandedCategory = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedItem == "Other") {
                                OutlinedTextField(
                                    value = customCategory,
                                    onValueChange = { customCategory = it },
                                    label = { Text("Specify Custom Category") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = expenditure,
                                onValueChange = { expenditure = it },
                                label = { Text("Item / Description") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Amount (₹)") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        // ==================== MONEY TRANSFER FIELDS ====================
                        else if (formMode == "Money Transfer") {
                            ExposedDropdownMenuBox(
                                expanded = expandedFromAccount,
                                onExpandedChange = { expandedFromAccount = !expandedFromAccount }
                            ) {
                                OutlinedTextField(
                                    value = fromAccount,
                                    onValueChange = {},
                                    label = { Text("From Account / Sender") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFromAccount) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedFromAccount,
                                    onDismissRequest = { expandedFromAccount = false }
                                ) {
                                    members.forEach { member ->
                                        DropdownMenuItem(
                                            text = { Text(member.name) },
                                            onClick = {
                                                fromAccount = member.name
                                                expandedFromAccount = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = expandedToAccount,
                                onExpandedChange = { expandedToAccount = !expandedToAccount }
                            ) {
                                OutlinedTextField(
                                    value = toAccount,
                                    onValueChange = {},
                                    label = { Text("To Account / Receiver") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToAccount) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedToAccount,
                                    onDismissRequest = { expandedToAccount = false }
                                ) {
                                    members.filter { it.name != fromAccount }.forEach { member ->
                                        DropdownMenuItem(
                                            text = { Text(member.name) },
                                            onClick = {
                                                toAccount = member.name
                                                expandedToAccount = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = transferAmount,
                                onValueChange = { transferAmount = it },
                                label = { Text("Transfer Amount (₹)") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = transferRemark,
                                onValueChange = { transferRemark = it },
                                label = { Text("Remark / Description (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        // ==================== RECEIPT / INCOME FIELDS ====================
                        else {
                            ExposedDropdownMenuBox(
                                expanded = expandedName,
                                onExpandedChange = { expandedName = !expandedName }
                            ) {
                                OutlinedTextField(
                                    value = selectedName,
                                    onValueChange = {},
                                    label = { Text("Select Account / Member") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedName) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedName,
                                    onDismissRequest = { expandedName = false }
                                ) {
                                    members.forEach { member ->
                                        DropdownMenuItem(
                                            text = { Text(member.name) },
                                            onClick = {
                                                selectedName = member.name
                                                expandedName = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = expandedReceiptCategory,
                                onExpandedChange = { expandedReceiptCategory = !expandedReceiptCategory }
                            ) {
                                OutlinedTextField(
                                    value = selectedReceiptCategory,
                                    onValueChange = {},
                                    label = { Text("Receipt Category") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReceiptCategory) },
                                    modifier = Modifier
                                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedReceiptCategory,
                                    onDismissRequest = { expandedReceiptCategory = false }
                                ) {
                                    dynamicReceiptCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                selectedReceiptCategory = cat
                                                expandedReceiptCategory = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedReceiptCategory == "Other") {
                                OutlinedTextField(
                                    value = customReceiptCategory,
                                    onValueChange = { customReceiptCategory = it },
                                    label = { Text("Specify Custom Receipt Type") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = expenditure,
                                onValueChange = { expenditure = it },
                                label = { Text("Source / Description (e.g. Salary Jan)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Receipt Amount (₹)") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // DATE PICKER (Common to all modes)
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
                                        val calendar = Calendar.getInstance()
                                        date?.let { calendar.time = it }
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedDate = Calendar.getInstance().apply {
                                                    set(year, month, dayOfMonth)
                                                }.time
                                                date = selectedDate
                                                dateInput = dateFormat.format(selectedDate)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick Date Selection Buttons Row (Previous, Today, Next)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Day Button (Step back 1 day further each click)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { stepDate(-1) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "⏪ Previous",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7B1FA2)
                                    )
                                }
                            }

                            // Today Button (Reset to current date)
                            val isToday = remember(dateInput) {
                                dateInput == dateFormat.format(Date())
                            }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { setTodayDate() },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isToday) Color(0xFF1565C0) else Color(0xFFE3F2FD)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "📍 Today",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isToday) Color.White else Color(0xFF1565C0)
                                    )
                                }
                            }

                            // Next Day Button (Step forward 1 day further each click)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { stepDate(1) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "⏩ Next",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // FORM VALIDATION CHECK
                        val isCategoryValid = if (selectedItem == "Other") customCategory.isNotBlank() else selectedItem.isNotBlank()
                        val isReceiptCategoryValid = if (selectedReceiptCategory == "Other") customReceiptCategory.isNotBlank() else selectedReceiptCategory.isNotBlank()

                        val isFormValid = when (formMode) {
                            "Regular Expenditure" -> selectedName.isNotBlank() && isCategoryValid &&
                                    expenditure.isNotBlank() && price.isNotBlank() && date != null
                            "Money Transfer" -> fromAccount.isNotBlank() && toAccount.isNotBlank() &&
                                    fromAccount != toAccount && transferAmount.isNotBlank() && date != null
                            else -> selectedName.isNotBlank() && isReceiptCategoryValid &&
                                    price.isNotBlank() && date != null
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                when (formMode) {
                                    "Regular Expenditure" -> {
                                        val finalCategory = if (selectedItem == "Other") customCategory.trim() else selectedItem
                                        val meal1 = Meal1(
                                            name = selectedName,
                                            item = finalCategory,
                                            expenditure = expenditure,
                                            price = price.toDoubleOrNull() ?: 0.0,
                                            meal = meal,
                                            date = date!!
                                        )
                                        viewModel.addMeal1(meal1)
                                        Toast.makeText(context, "Expenditure added successfully ✅", Toast.LENGTH_SHORT).show()

                                        selectedName = ""
                                        selectedItem = ""
                                        customCategory = ""
                                        expenditure = ""
                                        price = ""
                                        meal = 0
                                    }
                                    "Money Transfer" -> {
                                        val amount = transferAmount.toDoubleOrNull() ?: 0.0
                                        val parsedDate = date!!

                                        val senderRecord = Meal1(
                                            name = fromAccount,
                                            item = "Transfer Out",
                                            expenditure = transferRemark.ifBlank { "Transfer to $toAccount" },
                                            price = -amount,
                                            meal = 0,
                                            date = parsedDate
                                        )

                                        val receiverRecord = Meal1(
                                            name = toAccount,
                                            item = "Transfer In",
                                            expenditure = transferRemark.ifBlank { "Transfer from $fromAccount" },
                                            price = amount,
                                            meal = 0,
                                            date = parsedDate
                                        )

                                        viewModel.addMeal1(senderRecord)
                                        viewModel.addMeal1(receiverRecord)

                                        Toast.makeText(context, "Money transfer completed successfully ✅", Toast.LENGTH_SHORT).show()

                                        fromAccount = ""
                                        toAccount = ""
                                        transferAmount = ""
                                        transferRemark = ""
                                    }
                                    else -> {
                                        val finalReceiptCategory = if (selectedReceiptCategory == "Other") customReceiptCategory.trim() else selectedReceiptCategory
                                        val receiptItem = if (finalReceiptCategory.isNotBlank()) "Receipt: $finalReceiptCategory" else "Receipt"
                                        val finalExpenditure = expenditure.ifBlank { finalReceiptCategory.ifBlank { "Receipt" } }
                                        val amount = price.toDoubleOrNull() ?: 0.0

                                        val receiptRecord = Meal1(
                                            name = selectedName,
                                            item = receiptItem,
                                            expenditure = finalExpenditure,
                                            price = amount,
                                            meal = 0,
                                            date = date!!
                                        )
                                        viewModel.addMeal1(receiptRecord)
                                        Toast.makeText(context, "Receipt added successfully ✅", Toast.LENGTH_SHORT).show()

                                        selectedName = ""
                                        selectedReceiptCategory = ""
                                        customReceiptCategory = ""
                                        expenditure = ""
                                        price = ""
                                    }
                                }

                                date = Date()
                                dateInput = dateFormat.format(Date())
                                DriveBackupManager.syncAutomatically(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            enabled = isFormValid
                        ) {
                            Text(
                                text = when (formMode) {
                                    "Regular Expenditure" -> "Save Expenditure"
                                    "Money Transfer" -> "Execute Transfer"
                                    else -> "Save Receipt"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showNoMemberDialog) {
            AlertDialog(
                onDismissRequest = { showNoMemberDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("No Member or Account Found", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text("No members or personal accounts were found in EntityTable. You need to create at least one member or personal account before adding expenditures, transfers, or receipts.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoMemberDialog = false
                            onNavigateToAddMember()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Member / Account")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoMemberDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        GlobalCalculatorOverlay()
    }
}
