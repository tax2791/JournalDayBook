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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    fun loadUserDetails(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            val username = prefs.getString("username", "")?.trim() ?: ""
            val token = prefs.getString("sessionId", "")?.ifEmpty { prefs.getString("sessionId1", "") }?.trim() ?: ""
            val cachedEmail = prefs.getString("email", "")?.trim() ?: ""
            val cachedMessName = prefs.getString("messname", "")?.ifEmpty { prefs.getString("invoice_messname", "") }?.trim() ?: ""

            if (username.isEmpty()) {
                _message.value = "No user session found. Please log in."
                return@launch
            }

            _isLoading.value = true
            _message.value = "Loading user profile..."

            var fetchedUser: UserDetails? = null

            // 1. Try GET request first
            try {
                val responseGet = repository.getUserDetailsGet(username, token, cachedEmail)
                if (responseGet.isSuccessful) {
                    val body = responseGet.body()
                    fetchedUser = body?.user ?: body?.data
                }
            } catch (_: Exception) { }

            // 2. If GET returns null or unsuccessful, try POST request
            if (fetchedUser == null) {
                try {
                    val responsePost = repository.getUserDetailsPost(username, token, cachedEmail)
                    if (responsePost.isSuccessful) {
                        val body = responsePost.body()
                        fetchedUser = body?.user ?: body?.data
                    }
                } catch (_: Exception) { }
            }

            // 3. Process result or fallback to local session profile data
            if (fetchedUser != null) {
                _userDetails.value = fetchedUser
                _message.value = "Profile loaded successfully."

                // Cache to SharedPreferences
                prefs.edit()
                    .putString("invoice_username", fetchedUser.username ?: username)
                    .putString("invoice_messname", fetchedUser.messname ?: cachedMessName)
                    .putString("messname", fetchedUser.messname ?: cachedMessName)
                    .putString("address", fetchedUser.address ?: "")
                    .putString("gstin", fetchedUser.gstin ?: "")
                    .putString("email", fetchedUser.email ?: cachedEmail)
                    .putString("created_at", fetchedUser.created_at ?: "")
                    .putString("verified", fetchedUser.verified ?: "")
                    .apply()
            } else {
                // Construct fallback UserDetails from SharedPreferences so user profile always displays
                val fallbackUser = UserDetails(
                    messname = cachedMessName.ifEmpty { "User Profile" },
                    email = cachedEmail.ifEmpty { "-" },
                    username = username,
                    created_at = prefs.getString("created_at", "-"),
                    gstin = prefs.getString("gstin", "-"),
                    pan = prefs.getString("pan", "-"),
                    address = prefs.getString("address", "-"),
                    mobile = prefs.getString("mobile", "-"),
                    other_details = prefs.getString("other_details", "-"),
                    verified = prefs.getString("verified", "Active")
                )
                _userDetails.value = fallbackUser
                _message.value = "User Profile (Session Data)"
            }

            _isLoading.value = false
        }
    }
}
