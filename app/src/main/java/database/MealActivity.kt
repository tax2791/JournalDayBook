package database

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kushal.mealapp.MealScreen
import com.kushal.mealapp.database.AddMealForm
import com.kushal.mealapp.database.Listwise
import com.kushal.mealapp.database.MealTableScreen
import com.kushal.mealapp.database.MealViewModel
import com.kushal.mealapp.database.MealViewModelFactory
import com.kushal.mealapp.database.MemberForm
import com.kushal.mealapp.database.MyApp
import com.kushal.mealapp.ui.MealPieChart

class MealActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get MealDao instance from the database
        val mealDao = MyApp.Companion.getDatabase(this).mealDao()

        // Use ViewModelProvider factory
        val mealViewModel: MealViewModel by viewModels {
            MealViewModelFactory(mealDao)
        }

        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "meal_page"
            ) {
                composable("meal_page") {
                    MealScreen(viewModel = mealViewModel, navController = navController)
                }

                composable("add_meal_form") {
                    AddMealForm(viewModel = mealViewModel)
                }

                composable("summary_table") {
                    MealTableScreen(viewModel = mealViewModel)
                }

                composable("meal_form") {
                    ExpForm(viewModel = mealViewModel)
                }

                composable("member_form") {
                    MemberForm(viewModel = mealViewModel)
                }

                composable("summary_table1") {
                    SummaryTable(viewModel = mealViewModel)
                }

                composable("chart") {
                    MealPieChart(viewModel = mealViewModel)
                }

                composable("entity") {
                    EntityTable(viewModel = mealViewModel)
                }

                composable("listwise") {
                    Listwise(viewModel = mealViewModel)
                }
            }
        }
    }
}
