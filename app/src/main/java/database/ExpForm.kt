@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package database

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.database.Meal1
import com.kushal.mealapp.database.MealViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable

fun ExpForm(viewModel: MealViewModel) {
    val members by viewModel.allMembers.collectAsState(emptyList())
    var selectedName by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf("") }
    var expenditure by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var meal by remember { mutableIntStateOf(0) }

    var dateInput by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<Date?>(null) }
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val items = listOf("Vegetable", "Grocery", "Admin", "Travel", "Education", "Other")
    var expanded by remember { mutableStateOf(false) }
    var nameExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 6.dp,
            backgroundColor = Color(0xFFFDFDFD),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Add Expenditure",
                    style = MaterialTheme.typography.h6.copy(fontSize = 20.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // NAME DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = nameExpanded,
                    onExpandedChange = { nameExpanded = !nameExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        label = { Text("Select Name") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { nameExpanded = !nameExpanded }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = nameExpanded,
                        onDismissRequest = { nameExpanded = false }
                    ) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedName = member.name
                                    nameExpanded = false
                                }
                            ) {
                                Text(member.name)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CATEGORY DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedItem,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { expanded = !expanded }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        items.forEach { item ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedItem = item
                                    expanded = false
                                }
                            ) {
                                Text(item)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = expenditure,
                    onValueChange = { expenditure = it },
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // DATE PICKER
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedName.isNotBlank() && selectedItem.isNotBlank() &&
                            expenditure.isNotBlank() && price.isNotBlank() && date != null
                        ) {
                            val meal1 = Meal1(
                                name = selectedName,
                                item = selectedItem,
                                expenditure = expenditure,
                                price = price.toDouble(),
                                meal = meal,
                                date = date!!
                            )
                            viewModel.addMeal1(meal1)
                            Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show()

                            selectedName = ""
                            selectedItem = ""
                            expenditure = ""
                            price = ""
                            meal = 0
                            date = null
                            dateInput = ""
                        } else {
                            Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedName.isNotBlank() && selectedItem.isNotBlank() &&
                            expenditure.isNotBlank() && price.isNotBlank() && date != null
                ) {
                    Text("Update", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
