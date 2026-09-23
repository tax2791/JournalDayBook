package users

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserDetailsViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _userDetails = MutableStateFlow<UserDetails?>(null)
    val userDetails: StateFlow<UserDetails?> = _userDetails

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    fun loadUserDetails(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            val username = prefs.getString("username", "") ?: ""
            val token = prefs.getString("sessionId", "") ?: ""

            if (username.isNotEmpty() && token.isNotEmpty()) {
                val response = repository.getUserDetails(username, token)

                if (response.isSuccessful) {
                    val body = response.body()

                    _message.value = body?.message ?: ""

                    if (body?.status == "success") {
                        _userDetails.value = body.user

                        // ⭐ SAVE FOR INVOICE PDF
                        val editor = prefs.edit()
                        editor.putString("invoice_username", body.user?.username)
                        editor.putString("invoice_messname", body.user?.messname)
                        editor.apply()
                    }
                } else {
                    _message.value = "Server error"
                }
            }
        }
    }
    suspend fun loadUserDetailsSilently(context: Context) {
        val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val token = prefs.getString("sessionId", "") ?: ""

        if (username.isNotEmpty() && token.isNotEmpty()) {
            val response = repository.getUserDetails(username, token)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "success") {

                    // SAVE TO SHARED PREFS
                    prefs.edit()
                        .putString("messname", body.user?.messname ?: "")
                        .putString("address", body.user?.address ?: "")
                        .putString("gstin", body.user?.gstin ?: "")
                        .putString("email", body.user?.email ?: "")
                        .putString("created_at", body.user?.created_at ?: "")
                        .putString("verified", body.user?.verified ?: "")
                        .apply()
                }
            }
        }
    }

}