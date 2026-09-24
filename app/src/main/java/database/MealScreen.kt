package database

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MealScreen(viewModel: MealViewModel, navController: NavHostController) {
    // Collecting state for members/entities, meals, and deposits
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())
    val allMeals by viewModel.allMeals1.observeAsState(emptyList())
    val deposits by viewModel.allDeposits.collectAsState(initial = emptyList())

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()   // Keeps UI below camera & status bar
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween // Ensures spacing
        ) {
            // Grid Menu
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f) // Takes up available space but leaves room for disclaimer
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                item {
                    MenuItem(
                        "Add Member/Account",
                        Icons.Outlined.GroupAdd,
                        "member_form",
                        navController
                    )
                }
                item {
                    EntityMenuItem(
                        "Member/Entity Table",
                        Icons.Outlined.Person,
                        "entity",
                        navController,
                        allMembers
                    )
                }
                item {
                    MenuItem(
                        "Add Expenditure",
                        Icons.Outlined.Edit,
                        "meal_form",
                        navController
                    )
                }
                item {
                    SummaryMenuItem(
                        "Summary",
                        Icons.Outlined.BarChart,
                        "summary_table1",
                        navController,
                        allMeals
                    )
                }
                item { MenuItem("MealPieChart", Icons.Outlined.InsertChart, "chart", navController) }
                item { MenuItem("Listwise", Icons.Outlined.InsertChart, "listwise", navController) }
            }

            // Disclaimer
            Text(
                text = "Disclaimer: Offline Data is saved within the mobile, hence uninstalling the app will delete the data if it is not backed up.",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/**
 * Regular Menu Item
 */
@Composable
fun MenuItem(title: String, icon: ImageVector, route: String, navController: NavHostController) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { navController.navigate(route) },
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.body1)
        }
    }
}

/**
 * Entity Table Menu Item with Data Check
 */
@Composable
fun EntityMenuItem(
    title: String,
    icon: ImageVector,
    route: String,
    navController: NavHostController,
    allMembers: List<Member>
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                if (allMembers.isNotEmpty()) {
                    navController.navigate(route)
                } else {
                    Toast.makeText(context, "No members or personal accounts available!", Toast.LENGTH_SHORT).show()
                }
            },
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.body1)
        }
    }
}

/**
 * Summary Menu Item with Data Check
 */
@Composable
fun SummaryMenuItem(
    title: String,
    icon: ImageVector,
    route: String,
    navController: NavHostController,
    allMeals: List<Meal1>
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                if (allMeals.isNotEmpty()) {
                    navController.navigate(route)
                } else {
                    Toast.makeText(context, "No data available!", Toast.LENGTH_SHORT).show()
                }
            },
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.body1)
        }
    }
}