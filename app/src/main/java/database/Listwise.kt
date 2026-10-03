@file:OptIn(ExperimentalFoundationApi::class)

package database

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.kushal.mealapp.BannerAdView

@Composable
fun Listwise(viewModel: MealViewModel) {
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    val currentMonth = remember { SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()) }
    val currentYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) }

    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var filterText by remember { mutableStateOf("") }

    val filteredMeals = remember(allMeals1, selectedMonth, selectedYear, filterText) {
        allMeals1.filter { meal ->
            val mealMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(meal.date)
            val mealYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(meal.date)
            mealMonth == selectedMonth && mealYear == selectedYear &&
                    (filterText.isBlank() || meal.name.contains(filterText, ignoreCase = true))
        }
    }

    val totalAmount = remember(filteredMeals) {
        filteredMeals.sumOf { it.price }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BannerAdView()
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            Text(
                "Monthly Listwise Report",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Header with Month & Year Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val months = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                val years = (2020..2030).map { it.toString() }

                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenuComponent(
                        selectedValue = selectedMonth,
                        onValueSelected = { selectedMonth = it },
                        options = months
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenuComponent(
                        selectedValue = selectedYear,
                        onValueSelected = { selectedYear = it },
                        options = years
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE0F7FA)) // Light Cyan Background
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category / Item", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Amount (₹)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Table Content
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (filteredMeals.isEmpty()) {
                        item {
                            Text(
                                "No transactions found for $selectedMonth $selectedYear.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    itemsIndexed(filteredMeals) { index, meal ->
                        val formattedDate = formatDate(meal.date)
                        val formattedPrice = formatCurrency(meal.price)

                        val backgroundColor = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.item, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                Text(meal.expenditure, fontSize = 13.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(meal.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6A1B9A))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formattedPrice, 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = if (meal.price >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        if (index < filteredMeals.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }

                // Footer Total Row
                if (filteredMeals.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Monthly Amount:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatCurrency(totalAmount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalAmount >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuComponent(
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onValueSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
