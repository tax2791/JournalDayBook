@file:OptIn(ExperimentalMaterial3Api::class)

package database

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.BannerAdView
import com.kushal.mealapp.GlobalCalculatorOverlay
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
@Composable
fun SmartNoteScreen(viewModel: MealViewModel) {
    val context = LocalContext.current
    val allNotes by viewModel.allSmartNotes.collectAsState(initial = emptyList())

    // --- Search & Filter States ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("All") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var fromDateStr by remember { mutableStateOf("") }
    var toDateStr by remember { mutableStateOf("") }

    // --- Dialog States for Add / Edit / Delete ---
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<SmartNote?>(null) }
    var noteToDelete by remember { mutableStateOf<SmartNote?>(null) }

    // --- Available Tags ---
    val defaultTags = listOf("General", "Personal", "Work", "Urgent", "Finance", "Ideas")
    val allAvailableTags by remember(allNotes) {
        derivedStateOf {
            val customFromDb = allNotes.map { it.tag }.filter { it.isNotBlank() && it !in defaultTags }.distinct()
            listOf("All") + defaultTags + customFromDb
        }
    }

    // --- Filter Logic ---
    val filteredNotes by remember(allNotes, searchQuery, selectedTag, fromDateStr, toDateStr) {
        derivedStateOf {
            allNotes.filter { note ->
                val noteDateStr = dateFormat.format(note.date)

                val matchesSearch = searchQuery.isBlank() ||
                        note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true)

                val matchesTag = selectedTag == "All" || note.tag.equals(selectedTag, ignoreCase = true)

                val matchesDate = (fromDateStr.isEmpty() || noteDateStr >= fromDateStr) &&
                        (toDateStr.isEmpty() || noteDateStr <= toDateStr)

                matchesSearch && matchesTag && matchesDate
            }
        }
    }

    fun showDatePicker(initialDateStr: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        if (initialDateStr.isNotEmpty()) {
            try {
                dateFormat.parse(initialDateStr)?.let { calendar.time = it }
            } catch (_: Exception) {}
        }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time
                onDateSelected(dateFormat.format(selected))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

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
                            text = "📝 Smart Notes & Reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF002B49)
                        )
                        Text(
                            text = "⚡ Auto-Syncs with Google Drive",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { com.kushal.mealapp.FloatingCalculatorState.show() }) {
                        Text("🧮", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    noteToEdit = null
                    showNoteDialog = true
                },
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Note", fontWeight = FontWeight.Bold)
            }
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // --- Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes by title or content...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- Filter Panel (Date Range & Tag Chips) ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Date Filter Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker(fromDateStr) { fromDateStr = it } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (fromDateStr.isEmpty()) "From Date" else "From: $fromDateStr",
                                fontSize = 12.sp,
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.DateRange, contentDescription = "From Date", modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = { showDatePicker(toDateStr) { toDateStr = it } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (toDateStr.isEmpty()) "To Date" else "To: $toDateStr",
                                fontSize = 12.sp,
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.DateRange, contentDescription = "To Date", modifier = Modifier.size(16.dp))
                        }

                        if (fromDateStr.isNotEmpty() || toDateStr.isNotEmpty() || selectedTag != "All" || searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    fromDateStr = ""
                                    toDateStr = ""
                                    selectedTag = "All"
                                    searchQuery = ""
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Reset", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tag Filter Scrollable Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allAvailableTags.forEach { tag ->
                            val isSelected = selectedTag == tag
                            val tagColor = getTagColor(tag)

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTag = tag },
                                label = {
                                    Text(
                                        text = if (tag == "All") "🏷️ All Notes" else "# $tag",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tagColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Notes List Area ---
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (allNotes.isEmpty()) "No notes created yet." else "No notes match the selected filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Text(
                            text = "Tap '+ New Note' to create a smart note.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            dateFormat = dateFormat,
                            onEdit = {
                                noteToEdit = note
                                showNoteDialog = true
                            },
                            onDelete = {
                                noteToDelete = note
                            }
                        )
                    }
                }
            }
        }

        // --- Add / Edit Note Dialog ---
        if (showNoteDialog) {
            AddEditNoteDialog(
                initialNote = noteToEdit,
                defaultTags = defaultTags,
                dateFormat = dateFormat,
                onDismiss = { showNoteDialog = false },
                onSave = { title, content, tag, date ->
                    if (noteToEdit != null) {
                        val updated = noteToEdit!!.copy(
                            title = title,
                            content = content,
                            tag = tag,
                            date = date
                        )
                        viewModel.updateSmartNote(updated, context)
                        Toast.makeText(context, "Note updated & synced ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        val newNote = SmartNote(
                            title = title,
                            content = content,
                            tag = tag,
                            date = date
                        )
                        viewModel.addSmartNote(newNote, context)
                        Toast.makeText(context, "Note saved & synced ✅", Toast.LENGTH_SHORT).show()
                    }
                    showNoteDialog = false
                }
            )
        }

        // --- Delete Confirmation Dialog ---
        noteToDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = {
                    Text("🗑️ Delete Note", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                },
                text = {
                    Text("Are you sure you want to delete note \"${note.title}\"?\nThis will update your Google Drive backup automatically.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSmartNote(note, context)
                            Toast.makeText(context, "Note deleted & synced ✅", Toast.LENGTH_SHORT).show()
                            noteToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        GlobalCalculatorOverlay()
    }
}

// --- Note Card Item Component ---

@Composable
fun NoteCard(
    note: SmartNote,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tagColor = getTagColor(note.tag)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Tag Badge & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tagColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = "Tag",
                            tint = tagColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = note.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF002B49)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Note Content
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF444444),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Note Footer Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${dateFormat.format(note.date)}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// --- Add / Edit Note Dialog Component ---

@Composable
fun AddEditNoteDialog(
    initialNote: SmartNote?,
    defaultTags: List<String>,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tag: String, date: Date) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var selectedTag by remember { mutableStateOf(initialNote?.tag ?: "General") }
    var customTag by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf(initialNote?.date ?: Date()) }
    var dateText by remember { mutableStateOf(dateFormat.format(selectedDate)) }

    val isCustomTag = selectedTag == "Custom"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialNote != null) "✏️ Edit Smart Note" else "📝 New Smart Note",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF002B49)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Tag Selector Row
                Text("Select Tag / Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (defaultTags + "Custom").forEach { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, fontSize = 12.sp) }
                        )
                    }
                }

                if (isCustomTag) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        label = { Text("Specify Custom Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Content Field
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Details / Content") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Date Picker Field
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Note Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable {
                                val calendar = Calendar.getInstance().apply { time = selectedDate }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selected = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.time
                                        selectedDate = selected
                                        dateText = dateFormat.format(selected)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a note title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalTag = if (isCustomTag) customTag.trim().ifBlank { "General" } else selectedTag
                    onSave(title.trim(), content.trim(), finalTag, selectedDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                shape = RoundedCornerShape(10.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Save & Sync", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// --- Helper Functions ---

private fun getTagColor(tag: String): Color {
    return when (tag.lowercase(Locale.getDefault())) {
        "urgent" -> Color(0xFFD32F2F)   // Red
        "work" -> Color(0xFF1976D2)     // Blue
        "personal" -> Color(0xFF7B1FA2) // Purple
        "finance" -> Color(0xFF2E7D32)  // Green
        "ideas" -> Color(0xFFE65100)    // Orange
        else -> Color(0xFF00838F)       // Teal
    }
}
