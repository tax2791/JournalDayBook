@file:OptIn(ExperimentalFoundationApi::class)

package com.kushal.mealapp.database

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.formatCurrency
import database.formatDate
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Listwise(viewModel: MealViewModel) {
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    val currentMonth = remember { SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()) }
    val currentYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) }

    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var filterText by remember { mutableStateOf("") }

    val filteredMeals = allMeals1.filter { meal ->
        val mealMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(meal.date)
        val mealYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(meal.date)
        mealMonth == selectedMonth && mealYear == selectedYear &&
                (filterText.isBlank() || meal.name.contains(filterText, ignoreCase = true))
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Month & Year Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val months = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                val years = (2020..2030).map { it.toString() }

                DropdownMenu(
                    selectedMonth,
                    onValueSelected = { selectedMonth = it },
                    options = months
                )
                DropdownMenu(selectedYear, onValueSelected = { selectedYear = it }, options = years)
            }

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Category",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    Text("Item", fontSize = 14.sp, color = Color(0xFFFFA6C9))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Amount (Rs.)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    Text("Date", fontSize = 14.sp, color = Color(0xFF9D5FC9))
                }
            }

            // Table Content
            LazyColumn {
                itemsIndexed(filteredMeals) { _, meal ->
                    val formattedDate = formatDate(meal.date)
                    val formattedPrice = formatCurrency(meal.price)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(meal.item, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(meal.expenditure, fontSize = 14.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formattedPrice, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(formattedDate, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
    @Composable
    fun DropdownMenu(
        selectedValue: String,
        onValueSelected: (String) -> Unit,
        options: List<String>
    ) {
        var expanded by remember { mutableStateOf(false) }

        Box {
            Button(onClick = { expanded = true }) {
                Text(selectedValue)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { value ->
                    DropdownMenuItem(
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        }
                    ) {
                        Text(value)
                    }
                }
            }
        }
    }
