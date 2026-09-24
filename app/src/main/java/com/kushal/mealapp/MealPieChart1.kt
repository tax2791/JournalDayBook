package com.kushal.mealapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import database.MealViewModel

@Composable
fun MealPieChart1(viewModel: MealViewModel = viewModel()) {
    // Observe the LiveData from the ViewModel
    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    // Group meals by item and sum the prices
    val itemPriceMap = allMeals1.groupBy { it.item }
        .mapValues { entry -> entry.value.sumOf { it.price } }
        .filter { it.value > 0 } // Only keep items with positive price

    val colors = listOf(
        Color.Blue, Color.Red, Color.Green, Color.Yellow,
        Color.Cyan, Color.Magenta, Color.Gray
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Expenses Distribution",
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (itemPriceMap.isEmpty()) {
            Text("No data available for chart", modifier = Modifier.padding(8.dp))
        } else {
            PieChart1(
                data = itemPriceMap.toList(),
                colors = colors
            )
        }
    }
}

@Composable
fun PieChart1(data: List<Pair<String, Double>>, colors: List<Color>) {
    val total = data.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie Chart Canvas
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(250.dp)) {
                var startAngle = 0f
                data.forEachIndexed { index, (_, value) ->
                    val sweepAngle = (value / total * 360).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        style = Fill
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .verticalScroll(rememberScrollState())
        ) {
            data.forEachIndexed { index, (item, value) ->
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors[index % colors.size], shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$item: ₹${"%.2f".format(value)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
