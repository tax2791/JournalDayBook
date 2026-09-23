package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.core.content.edit

class ChatActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)

        // 🔁 Redirect to login if not logged in
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Please log in to access chat", Toast.LENGTH_SHORT).show()

            // Save redirect target
            sharedPrefs.edit { putString("redirectActivity", "ChatActivity") }

            startActivity(Intent(this, LoginComposeActivity::class.java))
            finish()
            return
        }

        // ✅ If logged in, show chat screen
        setContent {
            ChatRecordKeeper(
                username = username,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ChatRecordKeeper(
    username: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = remember { ChatViewModel(username) }
    var selectedTag by remember { mutableStateOf("All") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var messageText by remember { mutableStateOf("") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var chatToEdit by remember { mutableStateOf<ChatItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<ChatItem?>(null) }
    var messageTags by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val chatRecords by viewModel.chatRecords.collectAsState()
    val listState = rememberLazyListState()

    // Fetch chats on load
    LaunchedEffect(Unit) { viewModel.fetchChatsFromServer() }

    // Auto scroll
    LaunchedEffect(chatRecords.size) {
        listState.animateScrollToItem((chatRecords.size - 1).coerceAtLeast(0))
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFAAA1CD))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color(0xFFAAA1CD)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(Color(0xFFF3F4F6))
        ) {
            // ---- Top bar ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Back", color = Color.White) }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // 📅 DATE
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("📅 " + (selectedDate?.format(dateFormatter) ?: "All Dates"))
                    }

                    if (selectedDate != null) {
                        Button(
                            onClick = { selectedDate = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) { Text("Clear", color = Color.White) }
                    }

                    // 🏷 TAG DROPDOWN
                    var expandedTag by remember { mutableStateOf(false) }

                    val tagOptions = listOf("All") + chatRecords
                        .mapNotNull { it.tags }
                        .flatMap { it.split(",") }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()

                    Box {
                        Button(onClick = { expandedTag = true }) {
                            Text(selectedTag)
                        }

                        DropdownMenu(
                            expanded = expandedTag,
                            onDismissRequest = { expandedTag = false }
                        ) {
                            tagOptions.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        selectedTag = tag
                                        expandedTag = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Message input ----
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Write something...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Message input ----
            OutlinedTextField(
                value = messageTags,
                onValueChange = { messageTags = it },
                label = { Text("Tags something...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        val dateString = (selectedDate ?: LocalDate.now()).format(dateFormatter)
                        viewModel.sendChat(dateString, messageText, messageTags) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                messageText = ""
                                messageTags = ""
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                modifier = Modifier.align(Alignment.End)
            ) { Text("Submit", color = Color.White) }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedDate == null) "🗂 All Notes:" else "🗂 Notes for ${selectedDate!!.format(dateFormatter)}:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ---- Chat list ----
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState
            ) {
                val chatsToShow = chatRecords.filter { chat ->

                    val matchesDate =
                        selectedDate == null ||
                                chat.date == selectedDate!!.format(dateFormatter)

                    val matchesTag =
                        selectedTag == "All" ||
                                chat.tags?.split(",")?.any {
                                    it.trim().equals(selectedTag, ignoreCase = true)
                                } == true

                    matchesDate && matchesTag
                }
                items(chatsToShow, key = { it.id }) { chat ->
                    ChatItemCard(
                        chat = chat,
                        onDelete = {
                            chatToDelete = chat
                            showDeleteDialog = true
                        },
                        onEdit = {
                            chatToEdit = chat
                            showUpdateDialog = true
                        }
                    )
                }

                if (chatsToShow.isEmpty()) item {
                    Text("No notes found.", color = Color.Gray, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }

    // ---- Dialogs ----
    if (showDeleteDialog && chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    chatToDelete?.let { chat ->
                        viewModel.deleteChat(chat.id) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            title = { Text("Delete Chat") },
            text = { Text("Are you sure you want to delete this chat?") }
        )
    }

    if (showUpdateDialog && chatToEdit != null) {
        var updatedText by remember { mutableStateOf(TextFieldValue(chatToEdit!!.text)) }

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    viewModel.updateChat(chatToEdit!!.id, updatedText.text) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Cancel") }
            },
            title = { Text("Edit Chat") },
            text = {
                OutlinedTextField(
                    value = updatedText,
                    onValueChange = { updatedText = it },
                    label = { Text("Edit message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatItemCard(
    chat: ChatItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val dismissState = rememberDismissState()

    if (dismissState.isDismissed(DismissDirection.StartToEnd) ||
        dismissState.isDismissed(DismissDirection.EndToStart)
    ) {
        LaunchedEffect(chat) {
            onDelete()
            dismissState.reset()
        }
    }

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
        background = {
            val color = when (dismissState.dismissDirection) {
                DismissDirection.StartToEnd, DismissDirection.EndToStart -> Color(0xFFE53935)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        dismissContent = {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onEdit() })
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = chat.text,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chat.created_at ?: "",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chat.tags ?: "",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}
