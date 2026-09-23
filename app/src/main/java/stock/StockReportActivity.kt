package stock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

class StockReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: StockViewModel = viewModel()
                    // Fetch data as soon as screen opens
                    LaunchedEffect(Unit) {
                        viewModel.fetchStockList(this@StockReportActivity)
                    }
                    StockReportScreen(viewModel = viewModel)
                }
            }
        }
    }
}
