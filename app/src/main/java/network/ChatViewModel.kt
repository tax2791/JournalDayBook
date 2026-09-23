package com.kushal.mealapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val username: String) : ViewModel() {

    private val _chatRecords = MutableStateFlow<List<ChatItem>>(emptyList())
    val chatRecords: StateFlow<List<ChatItem>> = _chatRecords

    init {
        fetchChatsFromServer()
    }

    // Fetch all chats
    fun fetchChatsFromServer() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getChats(username)
                Log.d("ChatAPI", "Response: ${response.body()}")

                if (response.isSuccessful && response.body() != null) {
                    val chatList = response.body()!!.data
                    // Filter chats for this logged-in user
                    _chatRecords.value = chatList.filter { it.username == username }
                } else {
                    Log.e("ChatAPI", "Failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ChatAPI", "Error fetching chats", e)
            }
        }
    }


    // Add chat
    fun sendChat(date: String, text: String, tags: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.addChat(
                    action = "add",
                    date = date,
                    text = text,
                    tags = tags,
                    username = username // ✅ Pass actual username
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, response.body()?.message ?: "Chat added successfully")
                    fetchChatsFromServer()
                } else {
                    onResult(false, response.body()?.message ?: "Server error")
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.message}")
            }
        }
    }

    // Update chat
    fun updateChat(id: Int, text: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.updateChat(
                    action = "update",
                    id = id,
                    text = text,
                    username = username // ✅ Only allow updates by owner
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, response.body()?.message ?: "Chat updated successfully")
                    fetchChatsFromServer()
                } else {
                    onResult(false, response.body()?.message ?: "Server error")
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.message}")
            }
        }
    }

    // Delete chat
    fun deleteChat(id: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.deleteChat(
                    action = "delete",
                    id = id,
                    username = username // ✅ Only allow deletes by owner
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, response.body()?.message ?: "Chat deleted successfully")
                    fetchChatsFromServer()
                } else {
                    onResult(false, response.body()?.message ?: "Server error")
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.message}")
            }
        }
    }
}
