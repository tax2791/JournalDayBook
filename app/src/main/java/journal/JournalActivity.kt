package journal

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class JournalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JournalScreen()
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: TransactionViewModel = viewModel()) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // ✅ Retrieve logged-in username and token from SharedPreferences
    val sharedPref = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val username = sharedPref.getString("username", "") ?: ""
    val token = sharedPref.getString("sessionId", "") ?: ""

    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var selectedCategory by remember { mutableStateOf("Expenses") }
    var partyName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Debit") }

    val response by viewModel.response.collectAsState()
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val snackbarHost: @Composable () -> Unit = {
        SnackbarHost(hostState = snackbarHostState) { data ->
            val bgColor = when {
                data.visuals.message.contains("success", true) -> Color(0xFF4CAF50)
                data.visuals.message.contains("⚠️", true) -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
            Snackbar(containerColor = bgColor, contentColor = Color.White, snackbarData = data)
        }
    }

    // ✅ Auto-reset form and scroll to bottom on success
    LaunchedEffect(response) {
        if (response.contains("success", ignoreCase = true) ||
            response.contains("saved", ignoreCase = true)
        ) {
            // Reset form fields
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            partyName = ""
            description = ""
            amount = ""
            type = "Debit"
            selectedCategory = "Expenses"

            scope.launch {
                snackbarHostState.showSnackbar("✅ Transaction added successfully!")
                viewModel.fetchAllTransactions()

                // 👇 Smooth scroll to bottom (All Transactions)
                kotlinx.coroutines.delay(400) // small delay to allow list to update
                listState.animateScrollToItem(index =  transactions.size)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Journal Entry", fontWeight = FontWeight.Bold) })
        },
        snackbarHost = snackbarHost
    ) { padding ->

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // Transaction Type dropdown
                        val categories = listOf("Expenses", "Income", "Receipt", "Issue")
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Transaction Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            selectedCategory = category
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = partyName,
                            onValueChange = { partyName = it },
                            label = { Text("Party Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Narration") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Type: ", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            RadioButton(selected = type == "Debit", onClick = { type = "Debit" })
                            Text("Debit")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = type == "Credit", onClick = { type = "Credit" })
                            Text("Credit")
                        }

                        Button(
                            onClick = {
                                if (partyName.isNotEmpty() && amount.isNotEmpty()) {
                                    val transaction = Transaction(
                                        date = date,
                                        party_name = partyName,
                                        description = description,
                                        amount = amount,
                                        type = type,
                                        category = selectedCategory,
                                        username = username,
                                        token = token
                                    )
                                    viewModel.submitTransaction(transaction)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("⚠️ Please fill all required fields.")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.fetchAllTransactions()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Show Transactions", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions yet.",
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(transactions.reversed()) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${tx.date} - ${tx.party_name}", fontWeight = FontWeight.Bold)
                            Text(tx.description)
                            Text("Category: ${tx.category}", color = Color(0xFF1976D2))
                            Text(
                                "₹${tx.amount} (${tx.type})",
                                color = if (tx.type.equals("Credit", true))
                                    Color(0xFF4CAF50)
                                else
                                    Color(0xFFF44336)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // bottom padding
        }
    }
}
