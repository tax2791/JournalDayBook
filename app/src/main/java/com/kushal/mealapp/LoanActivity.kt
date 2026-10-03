package com.kushal.mealapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import database.LoanAccountScreen
import database.MealDao
import database.MealViewModel
import database.MealViewModelFactory
import database.MyApp

class LoanActivity : ComponentActivity() {

    private lateinit var mealViewModel: MealViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mealDao: MealDao = MyApp.getDatabase(applicationContext).mealDao()
        mealViewModel = ViewModelProvider(this, MealViewModelFactory(mealDao))[MealViewModel::class.java]

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoanAccountScreen(
                        viewModel = mealViewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
