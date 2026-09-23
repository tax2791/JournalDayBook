package com.kushal.mealapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.emptyList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("sessionId", null)

        if (token.isNullOrEmpty()) {
            val loginIntent = Intent(this, LoginComposeActivity::class.java)
            loginIntent.putExtra("redirectActivity", "MainActivity")
            startActivity(loginIntent)
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
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

    // ✅ NEW: Deposit target dropdown
    var depositToList by mutableStateOf(listOf<String>())
    var selectedDepositTo by mutableStateOf("Select")

    fun isSubmitEnabled(): Boolean {
        return (selectedName != "Select Name" && selectedDate.isNotEmpty() &&
                ((selectedItem == "Meal" && selectedMeal != "Select Meal") ||
                        (selectedItem == "Deposit" && selectedDepositTo != "Select" && price.isNotEmpty()) ||
                        (selectedItem != "Meal" && selectedItem != "Deposit" && price.isNotEmpty())))
    }
    // ✅ NEW: Prepare Deposit List
    fun prepareDepositList() {
        val members = nameList.filter {
            it != "Select Name" && it != selectedName
        }

        depositToList = listOf("Select") + members
        selectedDepositTo = "Select"
    }
    // ✅ Fetch Members
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
                        val adminName = "$username"
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

    // ✅ Fetch Items from DB
    fun fetchItems(context: Context, apiService: ApiService, username: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getItems(username)
                if (response.isSuccessful) {
                    val items = response.body()?.map { it.name } ?: emptyList()
                    itemList = if (items.isNotEmpty()) items else emptyList()
                } else {
                    Toast.makeText(context, "Failed to load items", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Insert New Item into DB
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
fun MainScreen(viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val apiService = remember { RetrofitInstance.api }

    val sharedPrefs = remember {
        context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    }
    val username = sharedPrefs.getString("username", "guest") ?: "guest"

    // Fetch member names and items
    LaunchedEffect(username) {
        viewModel.fetchMemberNames(context, apiService, username)
        viewModel.fetchItems(context, apiService, username)
    }

    val mealList = listOf("Select Meal", "1", "2", "-1", "-2")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Record Your Expenditure") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DatePickerField(selectedDate = viewModel.selectedDate) {
                viewModel.selectedDate = it
            }
            Spacer(Modifier.height(16.dp))

            DropdownField(
                label = "Select Member Name",
                options = viewModel.nameList,
                selected = viewModel.selectedName,
                onSelected = { viewModel.selectedName = it }
            )
            Spacer(Modifier.height(16.dp))

            // ✅ ITEM SECTION
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

                            // ✅ Populate deposit dropdown
                            viewModel.prepareDepositList()
                        }

                        else -> {
                            viewModel.selectedMeal = "Select Meal"
                            viewModel.price = ""
                        }
                    }
                }
            )
            Spacer(Modifier.height(16.dp))

            // ✅ Show Add New Item Field
            if (viewModel.selectedItem == "Add New Item") {
                OutlinedTextField(
                    value = viewModel.newItemName,
                    onValueChange = { viewModel.newItemName = it },
                    label = { Text("Enter New Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.insertNewItem(context, apiService, username) },
                    enabled = viewModel.newItemName.isNotBlank() && !viewModel.isAddingItem,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isAddingItem) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Item")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (viewModel.selectedItem == "Meal") {
                DropdownField(
                    label = "Select Meal Type",
                    options = mealList,
                    selected = viewModel.selectedMeal,
                    onSelected = { viewModel.selectedMeal = it }
                )
                Spacer(Modifier.height(16.dp))
            }
            if (viewModel.selectedItem == "Deposit") {
                Spacer(Modifier.height(16.dp))

                DropdownField(
                    label = "Deposit To",
                    options = viewModel.depositToList,
                    selected = viewModel.selectedDepositTo,
                    onSelected = { viewModel.selectedDepositTo = it }
                )
            }

            OutlinedTextField(
                value = viewModel.expenditure,
                onValueChange = { viewModel.expenditure = it },
                label = { Text("Details(optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = if (viewModel.selectedItem == "Meal") "0" else viewModel.price,
                onValueChange = { viewModel.price = it },
                label = { Text("Amount") },
                enabled = viewModel.selectedItem != "Meal",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    showPreviewDialog(context, viewModel) {
                        submitForm(context, apiService, viewModel)
                    }
                },
                enabled = viewModel.isSubmitEnabled() && !viewModel.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (viewModel.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(context, NameActivityAPI::class.java))
                }) { Text("Add Member") }

                OutlinedButton(onClick = {
                    context.startActivity(Intent(context, HomeActivity::class.java))
                }) { Text("Back Home") }

                OutlinedButton(onClick = { logout(context) }) { Text("Logout") }
            }

            var showCalculator by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(
                            Intent(context, Details::class.java)
                        )
                    },

                ) {
                    Text("Reports")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showCalculator = true
                    }
                ) {
                    Text("Calculator")
                }
            }

            if (showCalculator) {
                CalculatorPopup(
                    onDismiss = {
                        showCalculator = false
                    }
                )
            }
        }
    }
}

// ---------------------------- UI COMPONENTS ----------------------------
@Composable
fun DropdownField(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------- HELPERS ----------------------------
fun showPreviewDialog(
    context: Context,
    viewModel: MainViewModel,
    onConfirm: () -> Unit
) {
    val isDeposit = viewModel.selectedItem == "Deposit"

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

        // ✅ Show Meal only if needed
        if (viewModel.selectedItem == "Meal") {
            appendLine("Meal: ${viewModel.selectedMeal}")
        }

        // ✅ Show Deposit target only if Deposit
        if (isDeposit) {
            appendLine("Deposit To: ${viewModel.selectedDepositTo}")
        }

        appendLine("Expenditure: $finalExpenditure")
        appendLine("Price: $finalPrice")
        appendLine("Date: ${viewModel.selectedDate}")
    }

    android.app.AlertDialog.Builder(context)
        .setTitle("Preview Submission")
        .setMessage(msg)
        .setPositiveButton("Submit") { _, _ -> onConfirm() }
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

    // ✅ Deposit target handling
    val depositTo = if (isDeposit) viewModel.selectedDepositTo else ""

    // ✅ Optional: Better description for deposit
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
        depositTo = depositTo, // ✅ NEW PARAM
        token = token
    ).enqueue(object : Callback<JSONObject> {

        override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
            viewModel.isSubmitting = false

            if (response.isSuccessful) {
                Toast.makeText(context, "Form submitted successfully!", Toast.LENGTH_LONG).show()
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
        selectedDepositTo = "Select" // ✅ NEW
        depositToList = emptyList()  // ✅ NEW
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
