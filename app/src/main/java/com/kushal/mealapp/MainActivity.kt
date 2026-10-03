package com.kushal.mealapp

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("sessionId", null)
            ?: sharedPreferences.getString("sessionId1", null)

        if (token.isNullOrEmpty()) {
            val loginIntent = Intent(this, LoginComposeActivity::class.java)
            loginIntent.putExtra("redirectActivity", "MainActivity")
            startActivity(loginIntent)
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

// ---------------------------- VIEWMODEL ----------------------------
class MainViewModel : ViewModel() {
    var nameList by mutableStateOf(listOf("Select Name"))
    var itemList by mutableStateOf(listOf<String>())
    var selectedName by mutableStateOf("Select Name")
    var selectedItem by mutableStateOf("Select Item")
    var selectedMeal by mutableStateOf("Select Meal")
    var expenditure by mutableStateOf("")
    var price by mutableStateOf("")
    var selectedDate by mutableStateOf("")
    var newItemName by mutableStateOf("")
    var isSubmitting by mutableStateOf(false)
    var isAddingItem by mutableStateOf(false)

    var depositToList by mutableStateOf(listOf<String>())
    var selectedDepositTo by mutableStateOf("Select")

    fun isSubmitEnabled(): Boolean {
        return (selectedName != "Select Name" && selectedDate.isNotEmpty() &&
                ((selectedItem == "Meal" && selectedMeal != "Select Meal") ||
                        (selectedItem == "Deposit" && selectedDepositTo != "Select" && price.isNotEmpty()) ||
                        (selectedItem != "Meal" && selectedItem != "Deposit" && price.isNotEmpty())))
    }

    fun prepareDepositList() {
        val members = nameList.filter {
            it != "Select Name" && it != selectedName
        }

        depositToList = listOf("Select") + members
        selectedDepositTo = "Select"
    }

    fun fetchMemberNames(context: Context, apiService: ApiService, username: String) {
        apiService.getMemberNames(username).enqueue(object : Callback<MemberNamesResponse> {
            override fun onResponse(
                call: Call<MemberNamesResponse>,
                response: Response<MemberNamesResponse>
            ) {
                if (response.isSuccessful) {
                    val names = response.body()?.names ?: emptyList()
                    nameList = if (names.isNotEmpty()) {
                        listOf("Select Name") + names
                    } else {
                        val adminName = username
                        Toast.makeText(context, "No members found. Using $adminName", Toast.LENGTH_SHORT).show()
                        listOf("Select Name", adminName)
                    }
                } else {
                    Toast.makeText(context, "Failed to fetch members", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MemberNamesResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun fetchItems(context: Context, apiService: ApiService, username: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getItems(username)
                if (response.isSuccessful) {
                    val items = response.body()?.map { it.name } ?: emptyList()
                    itemList = items.ifEmpty { emptyList() }
                } else {
                    Toast.makeText(context, "Failed to load items", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun insertNewItem(context: Context, apiService: ApiService, username: String) {
        if (newItemName.isBlank()) {
            Toast.makeText(context, "Enter item name", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            isAddingItem = true
            try {
                val response = apiService.insertItem(username, newItemName)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Item added successfully!", Toast.LENGTH_SHORT).show()
                    newItemName = ""
                    fetchItems(context, apiService, username)
                } else {
                    Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isAddingItem = false
            }
        }
    }
}

// ---------------------------- COMPOSABLE UI ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val apiService = remember { RetrofitInstance.api }

    val sharedPrefs = remember {
        context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    }
    val username = sharedPrefs.getString("username", "guest") ?: "guest"
    var showCalculator by remember { mutableStateOf(false) }

    LaunchedEffect(username) {
        viewModel.fetchMemberNames(context, apiService, username)
        viewModel.fetchItems(context, apiService, username)
    }

    val mealList = listOf("Select Meal", "1", "2", "-1", "-2")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4F6F9),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Record Expenditure",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF002B49)
                        )
                        Text(
                            "Logged in as: $username",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, ProBillingActivity::class.java)) }) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "PRO Billing", tint = Color(0xFFFF8F00))
                    }
                    IconButton(onClick = { FloatingCalculatorState.show() }) {
                        Text("🧮", fontSize = 18.sp)
                    }
                    TextButton(onClick = { logout(context) }) {
                        Text("Logout", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Form Container Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Accent Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF57C00)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💸", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Add New Expenditure",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57C00)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Form Fields
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatePickerField(selectedDate = viewModel.selectedDate) {
                            viewModel.selectedDate = it
                        }

                        DropdownField(
                            label = "Select Member Name",
                            options = viewModel.nameList,
                            selected = viewModel.selectedName,
                            onSelected = { viewModel.selectedName = it }
                        )

                        DropdownField(
                            label = "Select Transaction Type",
                            options = viewModel.itemList + listOf("Add New Item"),
                            selected = viewModel.selectedItem,
                            onSelected = {
                                viewModel.selectedItem = it

                                when (it) {
                                    "Meal" -> {
                                        viewModel.price = "0"
                                        viewModel.selectedMeal = "Select Meal"
                                    }

                                    "Deposit" -> {
                                        viewModel.price = ""
                                        viewModel.selectedMeal = "Select Meal"
                                        viewModel.prepareDepositList()
                                    }

                                    else -> {
                                        viewModel.selectedMeal = "Select Meal"
                                        viewModel.price = ""
                                    }
                                }
                            }
                        )

                        if (viewModel.selectedItem == "Add New Item") {
                            OutlinedTextField(
                                value = viewModel.newItemName,
                                onValueChange = { viewModel.newItemName = it },
                                label = { Text("Enter New Item Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { viewModel.insertNewItem(context, apiService, username) },
                                enabled = viewModel.newItemName.isNotBlank() && !viewModel.isAddingItem,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (viewModel.isAddingItem) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Save Item", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (viewModel.selectedItem == "Meal") {
                            DropdownField(
                                label = "Select Meal Count / Type",
                                options = mealList,
                                selected = viewModel.selectedMeal,
                                onSelected = { viewModel.selectedMeal = it }
                            )
                        }

                        if (viewModel.selectedItem == "Deposit") {
                            DropdownField(
                                label = "Deposit Target Account",
                                options = viewModel.depositToList,
                                selected = viewModel.selectedDepositTo,
                                onSelected = { viewModel.selectedDepositTo = it }
                            )
                        }

                        OutlinedTextField(
                            value = viewModel.expenditure,
                            onValueChange = { viewModel.expenditure = it },
                            label = { Text("Details / Remarks (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = if (viewModel.selectedItem == "Meal") "0" else viewModel.price,
                            onValueChange = { viewModel.price = it },
                            label = { Text("Amount (₹)") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF57C00)) },
                            enabled = viewModel.selectedItem != "Meal",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (viewModel.selectedName == "Select Name") {
                                    Toast.makeText(context, "Please select a member name", Toast.LENGTH_SHORT).show()
                                } else if (viewModel.selectedDate.isEmpty()) {
                                    Toast.makeText(context, "Please select a transaction date", Toast.LENGTH_SHORT).show()
                                } else if (viewModel.selectedItem == "Select Item") {
                                    Toast.makeText(context, "Please select a transaction type", Toast.LENGTH_SHORT).show()
                                } else if (viewModel.selectedItem == "Meal" && viewModel.selectedMeal == "Select Meal") {
                                    Toast.makeText(context, "Please select a meal count", Toast.LENGTH_SHORT).show()
                                } else if (viewModel.selectedItem == "Deposit" && viewModel.selectedDepositTo == "Select") {
                                    Toast.makeText(context, "Please select target account for deposit", Toast.LENGTH_SHORT).show()
                                } else if (viewModel.selectedItem != "Meal" && viewModel.price.isEmpty()) {
                                    Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show()
                                } else {
                                    showPreviewDialog(context, viewModel) {
                                        submitForm(context, apiService, viewModel)
                                    }
                                }
                            },
                            enabled = !viewModel.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                        ) {
                            if (viewModel.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Submit Expenditure", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Quick Navigation & Actions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Quick Actions & Tools",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                val intent = Intent(context, NameActivityAPI::class.java)
                                activity?.startActivity(intent) ?: context.startActivity(intent)
                            }
                        ) {
                            Text("👥 Add Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                val intent = Intent(context, Details::class.java)
                                activity?.startActivity(intent) ?: context.startActivity(intent)
                            }
                        ) {
                            Text("📊 Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                val intent = Intent(context, LoanActivity::class.java)
                                activity?.startActivity(intent) ?: context.startActivity(intent)
                            }
                        ) {
                            Text("🤝 Loan Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                    }

                    // Back & Logout Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                activity?.finish()
                            }
                        ) {
                            Text("◀ Back", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            onClick = { logout(context) }
                        ) {
                            Text("Logout", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            GlobalCalculatorOverlay()
        }
    }
}

// ---------------------------- UI COMPONENTS ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            readOnly = true,
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DatePickerField(selectedDate: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    OutlinedTextField(
        value = if (selectedDate.isEmpty()) "Select Date" else selectedDate,
        onValueChange = {},
        label = { Text("Date") },
        trailingIcon = {
            IconButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDateSelected("$year-${month + 1}-$day") },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
            }
        },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

// ---------------------------- HELPERS ----------------------------
fun showPreviewDialog(
    context: Context,
    viewModel: MainViewModel,
    onConfirm: () -> Unit
) {
    val isDeposit = viewModel.selectedItem == "Deposit"
    val depositTo = if (isDeposit) viewModel.selectedDepositTo else ""

    val finalPrice = if (viewModel.selectedItem == "Meal") {
        "0"
    } else {
        viewModel.price.ifEmpty { "0" }
    }

    val finalExpenditure = if (isDeposit) {
        "Deposit to ${viewModel.selectedDepositTo}"
    } else {
        viewModel.expenditure.ifEmpty { "0" }
    }

    val msg = buildString {
        appendLine("Name: ${viewModel.selectedName}")
        appendLine("Item: ${viewModel.selectedItem}")

        if (viewModel.selectedItem == "Meal") {
            appendLine("Meal: ${viewModel.selectedMeal}")
        }

        if (isDeposit) {
            appendLine("Deposit To: ${viewModel.selectedDepositTo}")
        }

        appendLine("Expenditure: $finalExpenditure")
        appendLine("Price: ₹$finalPrice")
        appendLine("Date: ${viewModel.selectedDate}")
    }

    AlertDialog.Builder(context)
        .setTitle("Preview Submission")
        .setMessage(msg)
        .setPositiveButton("Submit") { _, _ ->
            val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            val isPro = prefs.getBoolean("isProVersion", false)

            if (isPro) {
                onConfirm()
            } else {
                // Redirect to Pro Billing Page when transaction is to be submitted
                val intent = Intent(context, ProBillingActivity::class.java).apply {
                    putExtra("name", viewModel.selectedName)
                    putExtra("item", viewModel.selectedItem)
                    putExtra("meal", viewModel.selectedMeal)
                    putExtra("expenditure", finalExpenditure)
                    putExtra("price", finalPrice)
                    putExtra("date", viewModel.selectedDate)
                    putExtra("depositTo", depositTo)
                    putExtra("hasPendingTransaction", true)
                }
                context.startActivity(intent)
                resetForm(viewModel)
            }
        }
        .setNegativeButton("Edit", null)
        .show()
}

fun submitForm(context: Context, apiService: ApiService, viewModel: MainViewModel) {

    val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val token = prefs.getString("sessionId", null)

    if (token.isNullOrEmpty()) {
        Toast.makeText(context, "Token not found. Please log in again.", Toast.LENGTH_LONG).show()
        return
    }

    val isDeposit = viewModel.selectedItem == "Deposit"

    val finalPrice = if (viewModel.selectedItem == "Meal") {
        "0"
    } else {
        viewModel.price.ifEmpty { "0" }
    }

    val depositTo = if (isDeposit) viewModel.selectedDepositTo else ""

    val finalExpenditure = if (isDeposit) {
        "Deposit to ${viewModel.selectedDepositTo}"
    } else {
        viewModel.expenditure.ifEmpty { "0" }
    }

    viewModel.isSubmitting = true

    apiService.submitForm(
        name = viewModel.selectedName,
        item = viewModel.selectedItem,
        meal = viewModel.selectedMeal,
        expenditure = finalExpenditure,
        price = finalPrice,
        date = viewModel.selectedDate,
        depositTo = depositTo,
        token = token
    ).enqueue(object : Callback<JSONObject> {

        override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
            viewModel.isSubmitting = false

            if (response.isSuccessful) {
                Toast.makeText(context, "Form submitted successfully! ✅", Toast.LENGTH_LONG).show()
                resetForm(viewModel)
            } else {
                Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: Call<JSONObject>, t: Throwable) {
            viewModel.isSubmitting = false
            Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
        }
    })
}

fun resetForm(viewModel: MainViewModel) {
    viewModel.apply {
        selectedName = "Select Name"
        selectedItem = "Select Item"
        selectedMeal = "Select Meal"
        selectedDepositTo = "Select"
        depositToList = emptyList()
        expenditure = ""
        price = ""
        selectedDate = ""
    }
}

fun logout(context: Context) {
    val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
    val intent = Intent(context, LoginComposeActivity::class.java)
    intent.putExtra("redirectActivity", "MainActivity")
    context.startActivity(intent)
}
