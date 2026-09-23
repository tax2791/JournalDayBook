package com.kushal.mealapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inside an Activity or Context
        val sharedPreferences = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val sessionToken = sharedPreferences.getString("sessionId", null)


        setContent {
            var combinedData by remember { mutableStateOf<CombinedDataResult?>(null) }
            var monthlyBalances by remember { mutableStateOf<List<MonthlyMemberBalance>>(emptyList()) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    retrieveCombinedData(
                        sessionToken = sessionToken,
                        onSuccess = { result ->
                            combinedData = result
                            monthlyBalances = processCombinedData(result.members, result.transactions)
                            isLoading = false
                        },
                        onError = { err ->
                            errorMessage = err
                            isLoading = false
                        }
                    )
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> LoadingView()
                    errorMessage != null -> ErrorView(errorMessage!!)
                    combinedData != null -> DashboardScreen(
                        combinedData = combinedData!!,
                        monthlyBalances = monthlyBalances
                    )
                }
            }
        }
    }
}
@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fetching data, please wait...")
        }
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Error: $message",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
