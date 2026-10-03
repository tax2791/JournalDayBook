package com.kushal.mealapp.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import database.MealViewModel

@Composable
fun MealPieChart1(viewModel: MealViewModel = viewModel()) {
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    // 1. FILTER FOR CURRENT MONTH & CURRENT YEAR ONLY
    val calendar = remember { Calendar.getInstance() }
    val currentMonth = remember { calendar.get(Calendar.MONTH) }
    val currentYear = remember { calendar.get(Calendar.YEAR) }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

    val currentMonthMeals = remember(allMeals1, currentMonth, currentYear) {
        allMeals1.filter { meal ->
            val cal = Calendar.getInstance().apply { time = meal.date }
            cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear &&
                    meal.price > 0 &&
                    !meal.item.startsWith("Receipt") &&
                    meal.item != "Transfer Out"
        }
    }

    // Group meals by item and sum positive prices
    val itemPriceMap = remember(currentMonthMeals) {
        currentMonthMeals.groupBy { it.item }
            .mapValues { entry -> entry.value.sumOf { it.price } }
            .filter { it.value > 0 }
    }

    val chartData = remember(itemPriceMap) {
        itemPriceMap.toList().sortedByDescending { it.second }
    }

    val colors = remember {
        listOf(
            Color(0xFF1E88E5), Color(0xFFF57C00), Color(0xFF43A047), Color(0xFF8E24AA),
            Color(0xFFD81B60), Color(0xFF00ACC1), Color(0xFF3949AB), Color(0xFF7CB342),
            Color(0xFFFB8C00), Color(0xFF00897B)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Current Month Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Current Month Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF002B49)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentMonthName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded for $currentMonthName",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                InteractivePieChartLayout(
                    data = chartData,
                    colors = colors
                )
            }
        }
    }
}

@Composable
fun InteractivePieChartLayout(data: List<Pair<String, Double>>, colors: List<Color>) {
    val totalExpense = remember(data) { data.sumOf { it.second } }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Small Touchable Pie Chart Canvas (140.dp)
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(140.dp)
                        .pointerInput(data, totalExpense) {
                            detectTapGestures { tapOffset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = tapOffset.x - centerX
                                val dy = tapOffset.y - centerY
                                val distance = sqrt(dx * dx + dy * dy)
                                val radius = min(centerX, centerY)

                                if (distance <= radius && totalExpense > 0) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    var cumulativeAngle = 0f
                                    data.forEachIndexed { index, (_, value) ->
                                        val sweep = (value / totalExpense * 360).toFloat()
                                        if (angle >= cumulativeAngle && angle < cumulativeAngle + sweep) {
                                            selectedIndex = if (selectedIndex == index) null else index
                                            return@detectTapGestures
                                        }
                                        cumulativeAngle += sweep
                                    }
                                }
                            }
                        }
                ) {
                    var startAngle = 0f
                    data.forEachIndexed { index, (_, value) ->
                        val sweepAngle = (value / totalExpense * 360).toFloat()
                        val isSelected = selectedIndex == index

                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            style = Fill,
                            alpha = if (selectedIndex == null || isSelected) 1f else 0.45f
                        )

                        if (isSelected) {
                            drawArc(
                                color = Color.White,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                style = Stroke(width = 5f)
                            )
                        }
                        startAngle += sweepAngle
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // RIGHT SIDE: Item Detail List with Percentages
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                data.forEachIndexed { index, (item, value) ->
                    val percentage = (value / totalExpense * 100)
                    val isSelected = selectedIndex == index
                    val color = colors[index % colors.size]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIndex = if (selectedIndex == index) null else index
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF8F9FA)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF1F2937)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${"%.0f".format(value)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "${"%.1f".format(percentage)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Detail Flash Card
        selectedIndex?.let { index ->
            val (selectedItem, selectedValue) = data[index]
            val percentage = (selectedValue / totalExpense * 100)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF002B49)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colors[index % colors.size], CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Category: $selectedItem",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Spent: ₹${"%.2f".format(selectedValue)}",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(percentage)}%",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
