package journal

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val _response = MutableStateFlow("")
    val response = _response.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions = _transactions.asStateFlow()

    private val context = application.applicationContext
    private val TAG = "TransactionViewModel"

    /** ✅ Load username + token */
    private fun getUserCredentials(): Pair<String, String> {
        val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val token = prefs.getString("sessionId", "") ?: ""
        Log.d(TAG, "🔑 Loaded credentials -> Username: $username | Token: $token")
        return username to token
    }

    /** ✅ Submit new transaction */
    fun submitTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                val (username, token) = getUserCredentials()

                if (username.isBlank() || token.isBlank()) {
                    _response.value = "Missing username or token!"
                    Log.e(TAG, "❌ Missing username/token in prefs")
                    return@launch
                }

                // ✅ attach credentials
                val fullTransaction = transaction.copy(username = username, token = token)
                Log.d(TAG, "📤 Sending transaction JSON: $fullTransaction")

                val result = RetrofitInstance2.api.saveTransaction(fullTransaction)
                _response.value = result.message

                Log.d(TAG, "✅ Server response: ${result.message}")

                if (result.status.equals("success", ignoreCase = true)) {
                    fetchAllTransactions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error submitting transaction", e)
                _response.value = "Error submitting transaction: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /** ✅ Fetch all transactions for user */
    fun fetchAllTransactions() {
        viewModelScope.launch {
            try {
                val (username, token) = getUserCredentials()

                if (username.isBlank() || token.isBlank()) {
                    _response.value = "Missing username or token!"
                    Log.e(TAG, "❌ Missing username/token in prefs")
                    return@launch
                }

                val credentials = mapOf("username" to username, "token" to token)
                Log.d(TAG, "📥 Fetching transactions with: $credentials")

                val result = RetrofitInstance2.api.getAllTransactions(credentials)

                if (result.status.equals("success", ignoreCase = true)) {
                    _transactions.value = result.transactions.reversed()
                    Log.d(TAG, "📦 Transactions fetched: ${result.transactions.size}")
                } else {
                    _transactions.value = emptyList()
                    _response.value = "No transactions found."
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching transactions", e)
                _response.value = "Error fetching transactions: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }
}
