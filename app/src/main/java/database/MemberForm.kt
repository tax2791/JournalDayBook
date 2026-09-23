package com.kushal.mealapp.database

import android.app.DatePickerDialog
import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
@Composable
fun MemberForm(viewModel: MealViewModel) {
    val context = LocalContext.current

    // Input state variables
    var name by remember { mutableStateOf("") }
    var joinDate by remember { mutableStateOf<Date?>(null) }
    var exitDate by remember { mutableStateOf<Date?>(null) }
    val createdDate = remember { System.currentTimeMillis() } // Auto-generated timestamp

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Function to show the Date Picker
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
            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Join Date Picker
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

            // Exit Date Picker (Optional)
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

            // Submit Button
            Button(
                onClick = {
                    if (name.isNotEmpty() && joinDate != null) {
                        val member = Member(
                            id = 0,
                            name = name,
                            joinDate = joinDate!!,
                            createdDate = createdDate, // Auto-generated timestamp
                            exitDate = exitDate
                        )
                        viewModel.insertMember(member)

                        // Reset fields after submission
                        name = ""
                        joinDate = null
                        exitDate = null
                    }
                },
                enabled = name.isNotEmpty() && joinDate != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Member")
            }
        }
    }
}