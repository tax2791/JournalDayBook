package database

import android.app.DatePickerDialog
import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun MemberForm(viewModel: MealViewModel) {
    val context = LocalContext.current

    // Form mode: "Group Member" or "Personal Account"
    var entryType by remember { mutableStateOf("Group Member") }
    var expandedEntryType by remember { mutableStateOf(false) }
    val entryTypes = listOf("Group Member", "Personal Account")

    // Common fields
    var name by remember { mutableStateOf("") }
    var openingBalance by remember { mutableStateOf("") }

    // Personal Account specific fields
    var accountType by remember { mutableStateOf("Savings") }
    var expandedAccountType by remember { mutableStateOf(false) }
    val accountTypes = listOf("Savings", "Deposit", "Credit Card", "Cash Wallet", "Current", "Other")

    // Group Member specific fields
    var joinDate by remember { mutableStateOf<Date?>(null) }
    var exitDate by remember { mutableStateOf<Date?>(null) }
    val createdDate = remember { System.currentTimeMillis() }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Entry Type Dropdown (Group vs Personal)
            ExposedDropdownMenuBox(
                expanded = expandedEntryType,
                onExpandedChange = { expandedEntryType = !expandedEntryType },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = entryType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Record Type") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedEntryType,
                    onDismissRequest = { expandedEntryType = false }
                ) {
                    entryTypes.forEach { type ->
                        DropdownMenuItem(onClick = {
                            entryType = type
                            expandedEntryType = false
                        }) {
                            Text(text = type)
                        }
                    }
                }
            }

            // 2. Name Input (Member Name or Account Name)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (entryType == "Group Member") "Member Name" else "Account Label Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Conditional Fields for Personal Accounts
            if (entryType == "Personal Account") {
                ExposedDropdownMenuBox(
                    expanded = expandedAccountType,
                    onExpandedChange = { expandedAccountType = !expandedAccountType },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = accountType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Sub-Type") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAccountType,
                        onDismissRequest = { expandedAccountType = false }
                    ) {
                        accountTypes.forEach { type ->
                            DropdownMenuItem(onClick = {
                                accountType = type
                                expandedAccountType = false
                            }) {
                                Text(text = type)
                            }
                        }
                    }
                }
            }

            // 4. Opening Balance (₹)
            OutlinedTextField(
                value = openingBalance,
                onValueChange = { openingBalance = it },
                label = { Text("Opening Balance (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Conditional Fields for Group Members (Join/Exit Dates)
            if (entryType == "Group Member") {
                OutlinedTextField(
                    value = joinDate?.let { dateFormat.format(it) } ?: "Select Join Date",
                    onValueChange = {},
                    label = { Text("Join Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showDatePicker { joinDate = it } }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker { joinDate = it } }
                )

                OutlinedTextField(
                    value = exitDate?.let { dateFormat.format(it) } ?: "Select Exit Date (Optional)",
                    onValueChange = {},
                    label = { Text("Exit Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showDatePicker { exitDate = it } }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker { exitDate = it } }
                )
            }

            // 6. Submit Button
            val isFormValid = if (entryType == "Group Member") {
                name.isNotEmpty() && joinDate != null
            } else {
                name.isNotEmpty()
            }

            Button(
                onClick = {
                    val balanceValue = openingBalance.toDoubleOrNull() ?: 0.0

                    if (entryType == "Group Member") {
                        // Handle Group Member Insertion logic
                        // viewModel.insertMember(...)
                    } else {
                        // Handle Personal Account Insertion logic
                        // viewModel.insertAccount(...)
                    }

                    // Reset Fields
                    name = ""
                    openingBalance = ""
                    joinDate = null
                    exitDate = null
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entryType == "Group Member") "Add Group Member" else "Add Personal Account")
            }
        }
    }
}