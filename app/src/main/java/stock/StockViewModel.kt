package stock

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.internal.Contexts.getApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StockViewModel : ViewModel() {
    private val repository = StockRepository()

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private val _stockList = MutableStateFlow<List<StockTransaction>>(emptyList())
    val stockList: StateFlow<List<StockTransaction>> = _stockList

    fun submitStock(stock: StockTransaction) {
        viewModelScope.launch {
            try {
                val result = repository.submitStock(stock)
                if (result.isSuccessful && result.body() != null) {
                    _response.value = result.body()!!.message.toString()
                } else {
                    _response.value = "Error: ${result.code()}"
                }
            } catch (e: Exception) {
                _response.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun fetchStockList(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
                val username = prefs.getString("username", "") ?: ""
                val token = prefs.getString("sessionId", "") ?: ""

                if (username.isNotEmpty() && token.isNotEmpty()) {
                    val result = repository.getStockList(username, token)
                    if (result.isSuccessful && result.body() != null) {
                        _stockList.value = result.body()!!.stockTransaction
                    } else {
                        _response.value = "Error: ${result.code()} - ${result.message()}"
                    }
                } else {
                    _response.value = "Error: Missing username or token in session"
                }
            } catch (e: Exception) {
                _response.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    private val _itemNames = MutableStateFlow<List<String>>(emptyList())
    val itemNames: StateFlow<List<String>> = _itemNames


    fun fetchItemNames(username: String) {
        viewModelScope.launch {
            try {
                val result = repository.getItemNames(username)
                if (result.isSuccessful && result.body()?.status == "success") {
                    val itemsWithQty = result.body()?.items?.map { item ->
                        "${item.item_name}(qty:${item.quantity})"
                    } ?: emptyList()
                    _itemNames.value = itemsWithQty
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



}