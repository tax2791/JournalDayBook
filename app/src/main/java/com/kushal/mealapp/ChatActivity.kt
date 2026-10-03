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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class ChatActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)

        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Please log in to access chat & notes", Toast.LENGTH_SHORT).show()
            sharedPrefs.edit { putString("redirectActivity", "ChatActivity") }
            startActivity(Intent(this, LoginComposeActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatRecordKeeper(
                        username = username,
                        onBack = { finish() }
                    )
                }
            }
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
    var messageTags by remember { mutableStateOf("") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var chatToEdit by remember { mutableStateOf<ChatItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<ChatItem?>(null) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val chatRecords by viewModel.chatRecords.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.fetchChatsFromServer() }

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
            .systemBarsPadding(),
        containerColor = Color(0xFFF4F6F9),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Personal Notes & Journal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF002B49)
                        )
                        Text(
                            "Logged in as: $username",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchChatsFromServer() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF1565C0))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F6F9))
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BannerAdView()
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Scrollable Filter Bar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📅 " + (selectedDate?.format(dateFormatter) ?: "All Dates"), fontSize = 12.sp, color = Color(0xFF1565C0))
                    }

                    if (selectedDate != null) {
                        OutlinedButton(
                            onClick = { selectedDate = null },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear Date", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    var expandedTag by remember { mutableStateOf(false) }

                    val tagOptions = remember(chatRecords) {
                        listOf("All") + chatRecords
                            .mapNotNull { it.tags }
                            .flatMap { it.split(",") }
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                    }

                    Box {
                        OutlinedButton(
                            onClick = { expandedTag = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🏷 Tag: $selectedTag", fontSize = 12.sp, color = Color(0xFF1565C0))
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

            // Note Input Card Form
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "✍️ Create New Note",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF002B49)
                    )

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Write your note / description...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp, max = 110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = messageTags,
                        onValueChange = { messageTags = it },
                        label = { Text("Tags (comma separated, e.g. Work, Personal)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

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
                                Toast.makeText(context, "Please enter a message or note", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Save Note", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Text(
                text = if (selectedDate == null) "🗂 All Saved Notes (${chatRecords.size})" else "🗂 Notes for ${selectedDate!!.format(dateFormatter)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF002B49),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Chat Records List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState
            ) {
                val chatsToShow = chatRecords.filter { chat ->
                    val matchesDate = selectedDate == null || chat.date == selectedDate!!.format(dateFormatter)
                    val matchesTag = selectedTag == "All" || chat.tags?.split(",")?.any {
                        it.trim().equals(selectedTag, ignoreCase = true)
                    } == true
                    matchesDate && matchesTag
                }

                if (chatsToShow.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No saved notes found for selected filters.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
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
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog && chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        chatToDelete?.let { chat ->
                            viewModel.deleteChat(chat.id) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            title = { Text("Delete Note", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this note?") }
        )
    }

    // Update Dialog
    if (showUpdateDialog && chatToEdit != null) {
        var updatedText by remember { mutableStateOf(TextFieldValue(chatToEdit!!.text)) }

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        viewModel.updateChat(chatToEdit!!.id, updatedText.text) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Cancel") }
            },
            title = { Text("Edit Note", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = updatedText,
                    onValueChange = { updatedText = it },
                    label = { Text("Edit note text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                    .clip(RoundedCornerShape(14.dp))
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
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onEdit() })
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📝", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = chat.date,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = chat.text,
                        color = Color(0xFF1F2937),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Tag Badges
                    if (!chat.tags.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            chat.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE3F2FD))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                        }
                    }

                    if (!chat.created_at.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Created: ${chat.created_at}",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    )
}
