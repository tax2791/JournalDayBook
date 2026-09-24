package database

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MealPieChart(viewModel: MealViewModel = viewModel()) {

    // Collect Flow from ViewModel

    val allMeals1 by viewModel.allMeals1.observeAsState(emptyList())

    // Group meals by item name and sum their prices
    val itemPriceMap = allMeals1.groupBy { it.item }
        .mapValues { entry -> entry.value.sumOf { it.price } }
        .filter { it.value > 0 } // Remove zero-priced items

    val colors = listOf(
        Color.Blue, Color.Red, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray
    )
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Price Distribution", style = MaterialTheme.typography.h6)

            Spacer(modifier = Modifier.height(16.dp))

            if (itemPriceMap.isEmpty()) {
                Text("No data available for chart", modifier = Modifier.padding(8.dp))
            } else {
                PieChart(
                    data = itemPriceMap.toList(),
                    colors = colors
                )
            }
        }
    }
}
@Composable
fun PieChart(data: List<Pair<String, Double>>, colors: List<Color>) {
    val total = data.sumOf { it.second }

    // Remember animated values for slices
    val animatedAngles = data.map { (_, value) ->
        animateFloatAsState(
            targetValue = (value / total * 360).toFloat(),
            animationSpec = tween(durationMillis = 800)
        )
    }

    // Remember animated values for legend numbers
    val animatedValues = data.map { (_, value) ->
        animateFloatAsState(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .background(Color.LightGray, shape = CircleShape)
                .clip(CircleShape)
        ) {
            var startAngle = 0f
            animatedAngles.forEachIndexed { index, animAngle ->
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = animAngle.value,
                    useCenter = true,
                    style = Fill
                )
                startAngle += animAngle.value
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        data.forEachIndexed { index, (item, _) ->
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
                    text = "$item: ₹${"%.2f".format(animatedValues[index].value)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
