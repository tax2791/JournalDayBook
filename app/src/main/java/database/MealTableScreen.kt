@file:OptIn(ExperimentalFoundationApi::class)

package com.kushal.mealapp.database

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Surface
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
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

@Composable
fun MealTableScreen(viewModel: MealViewModel) {
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())
    var filterText by remember { mutableStateOf("") } // State for the filter text

    // Filter meals based on the name
    val filteredMeals = if (filterText.isBlank()) {
        allMeals1
    } else {
        allMeals1.filter { meal ->
            meal.name.contains(filterText, ignoreCase = true)
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Filter TextField
        BasicTextField(
            value = filterText,
            onValueChange = { filterText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(BorderStroke(1.dp, Color.Black))
                .padding(8.dp),
            decorationBox = { innerTextField ->
                if (filterText.isEmpty()) {
                    Text("Search by name...", color = Color.Gray)
                }
                innerTextField()
            }
        )

        // Table
        MealTable(
            meals = filteredMeals, // Pass the filtered list
        )
    }
}
    }

@Composable
fun MealTable(
    meals: List<Meal1>,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Column(modifier = Modifier.padding(16.dp)) {
        // Table start
        Column(modifier = Modifier.fillMaxSize()) {
            // Table Header (Fixed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "S.No.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    Text("Date", fontSize = 14.sp, color = Color(0xFFFFA6C9))
                    Text("Name", fontSize = 14.sp, color = Color(0xFFFFA6C9))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Item",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    Text("Remark", fontSize = 14.sp, color = Color(0xFF9D5FC9))
                    Text(
                        "Amount (Rs.)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                }
            }

            // Scrollable Table Content
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(meals) { index, meal ->
                    val formattedDate = formatDate(meal.date)
                    val formattedPrice = formatCurrency(meal.price)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                (index + 1).toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            ) // S.No.
                            Text(formattedDate, fontSize = 14.sp, color = Color.Gray) // Date
                            Text(meal.name, fontSize = 14.sp, color = Color.Gray) // Name
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(meal.item, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(meal.expenditure, fontSize = 14.sp, color = Color.Gray)
                            Text(formattedPrice, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}


