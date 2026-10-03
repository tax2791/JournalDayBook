@file:OptIn(ExperimentalFoundationApi::class)

package database

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.BannerAdView
import com.kushal.mealapp.DriveBackupManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityTable(viewModel: MealViewModel) {
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())
    var selectedMemberForAction by remember { mutableStateOf<Member?>(null) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredMembers = remember(allMembers, searchQuery) {
        if (searchQuery.isBlank()) {
            allMembers
        } else {
            allMembers.filter { member ->
                member.name.contains(searchQuery, ignoreCase = true) ||
                        (member.accountName?.contains(searchQuery, ignoreCase = true) == true) ||
                        member.type.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalBalance = remember(filteredMembers) {
        filteredMembers.sumOf { it.openingBalance }
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
                            "Member & Entity Directory",
                            color = Color(0xFF002B49),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${filteredMembers.size} Records (Tap for details, long press to edit / delete)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
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
                .padding(horizontal = 12.dp)
        ) {
            // Search Bar & Filter Controls
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, label or type...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { searchQuery = "" },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            // Main Table Card Container
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    MemberTable(
                        members = filteredMembers,
                        totalBalance = totalBalance,
                        onRowLongClick = { member ->
                            selectedMemberForAction = member
                        }
                    )
                }
            }
        }

        // Long Press Action Choice Dialog (Edit / Delete)
        selectedMemberForAction?.let { member ->
            MemberActionMenuDialog(
                member = member,
                onDismiss = { selectedMemberForAction = null },
                onEdit = {
                    memberToEdit = member
                    selectedMemberForAction = null
                },
                onDelete = {
                    memberToDelete = member
                    selectedMemberForAction = null
                }
            )
        }

        // Edit Dialog
        memberToEdit?.let { member ->
            EditMemberDialog(
                member = member,
                onDismiss = { memberToEdit = null },
                onSave = { updatedMember ->
                    viewModel.updateMember(updatedMember)
                    Toast.makeText(context, "Updated '${member.name}' successfully ✅", Toast.LENGTH_SHORT).show()
                    DriveBackupManager.syncAutomatically(context)
                    memberToEdit = null
                }
            )
        }

        // Delete Confirmation Dialog
        memberToDelete?.let { member ->
            AlertDialog(
                onDismissRequest = { memberToDelete = null },
                title = { Text("Delete Member / Account", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete \"${member.name}\"? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMember(member)
                            Toast.makeText(context, "Deleted \"${member.name}\" successfully ✅", Toast.LENGTH_SHORT).show()
                            DriveBackupManager.syncAutomatically(context)
                            memberToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { memberToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MemberActionMenuDialog(
    member: Member,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Member / Account Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF002B49)
                )
                Text(
                    text = "${member.name} (${member.type})",
                    fontSize = 12.sp,
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit Option Button
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Member / Account Data", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Delete Option Button
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Member / Account Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberDialog(
    member: Member,
    onDismiss: () -> Unit,
    onSave: (Member) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(member.name) }
    var entryType by remember { mutableStateOf(member.type) }
    var accountName by remember { mutableStateOf(member.accountName ?: "") }
    var accountType by remember { mutableStateOf(member.accountType ?: "Savings") }
    var openingBalance by remember { mutableStateOf(member.openingBalance.toString()) }
    var joinDate by remember { mutableStateOf<Date?>(member.joinDate ?: Date()) }
    var exitDate by remember { mutableStateOf<Date?>(member.exitDate) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        joinDate?.let { calendar.time = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    var expandedType by remember { mutableStateOf(false) }
    var expandedAccountType by remember { mutableStateOf(false) }
    val typeOptions = listOf("Group Member", "Personal Account")
    val accountTypes = listOf("Savings", "Deposit", "Credit Card", "Cash Wallet", "Current", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit '${member.name}'",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF002B49)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member / Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Record Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = entryType,
                        onValueChange = {},
                        label = { Text("Record Type") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        typeOptions.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    entryType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                if (entryType == "Personal Account") {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Account Label Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedAccountType,
                        onExpandedChange = { expandedAccountType = !expandedAccountType }
                    ) {
                        OutlinedTextField(
                            value = accountType,
                            onValueChange = {},
                            label = { Text("Account Sub-Type") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountType) },
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAccountType,
                            onDismissRequest = { expandedAccountType = false }
                        ) {
                            accountTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        accountType = type
                                        expandedAccountType = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Opening Balance
                OutlinedTextField(
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = { Text("Opening Balance (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1565C0)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Join Date
                OutlinedTextField(
                    value = joinDate?.let { dateFormat.format(it) } ?: "Select Opening Date",
                    onValueChange = {},
                    label = { Text("Opening / Join Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showDatePicker { joinDate = it } }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Exit Date (Optional)
                if (entryType == "Group Member") {
                    OutlinedTextField(
                        value = exitDate?.let { dateFormat.format(it) } ?: "Select Exit Date (Optional)",
                        onValueChange = {},
                        label = { Text("Exit Date (Optional)") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Exit Date",
                                modifier = Modifier.clickable { showDatePicker { exitDate = it } }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updatedMember = member.copy(
                        name = name.trim(),
                        type = entryType,
                        accountName = if (entryType == "Personal Account") accountName.trim().ifEmpty { name.trim() } else null,
                        accountType = if (entryType == "Personal Account") accountType else null,
                        openingBalance = openingBalance.toDoubleOrNull() ?: member.openingBalance,
                        joinDate = joinDate,
                        exitDate = exitDate
                    )

                    onSave(updatedMember)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun MemberTable(
    members: List<Member>,
    totalBalance: Double,
    onRowLongClick: (Member) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var expandedMemberId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sticky Table Header Row
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE0F7FA))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell("S.No.", Modifier.weight(0.6f), isHeader = true, textAlign = TextAlign.Center)
                    TableCell("Name / Account", Modifier.weight(2.2f), isHeader = true, textAlign = TextAlign.Start)
                    TableCell("Opening Bal (₹)", Modifier.weight(1.6f), isHeader = true, textAlign = TextAlign.End)
                }
            }

            // Data Rows
            if (members.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No members or entity records found.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                itemsIndexed(members) { index, member ->
                    val isExpanded = expandedMemberId == member.id
                    val backgroundColor = if (index % 2 == 0) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    val createdDateStr = remember(member.createdDate) {
                        dateFormat.format(Date(member.createdDate))
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .combinedClickable(
                                    onClick = {
                                        expandedMemberId = if (isExpanded) null else member.id
                                    },
                                    onLongClick = {
                                        onRowLongClick(member)
                                    }
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell((index + 1).toString(), Modifier.weight(0.6f), textColor = Color.DarkGray, textAlign = TextAlign.Center)

                            // Name / Account with expand indicator
                            TableCell(
                                text = "${member.name} ${if (isExpanded) "▲" else "▼"}",
                                modifier = Modifier.weight(2.2f),
                                textColor = Color(0xFF1565C0),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            )

                            TableCell(
                                text = "₹${member.openingBalance}",
                                modifier = Modifier.weight(1.6f),
                                textColor = if (member.openingBalance >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            )
                        }

                        // Expanded Dropdown Details View
                        AnimatedVisibility(visible = isExpanded) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "📋 Account & Member Details",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1565C0)
                                        )
                                        Text(
                                            "💡 Long press row to edit / delete",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFF90CAF9))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailBadgeItem("Record Type", member.type)
                                            DetailItem("Account Label", member.accountName ?: "N/A")
                                            DetailItem("Sub-Type", member.accountType ?: "N/A")
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem("Join Date", member.joinDate?.let { dateFormat.format(it) } ?: "N/A")
                                            DetailItem("Created Date", createdDateStr)
                                            DetailItem("Exit Date", member.exitDate?.let { dateFormat.format(it) } ?: "N/A")
                                        }
                                    }
                                }
                            }
                        }

                        if (index < members.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }

                // Summary Totals Footer
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("TOTAL", Modifier.weight(0.6f), isHeader = true, textAlign = TextAlign.Center)
                        TableCell("${members.size} Records", Modifier.weight(2.2f), isHeader = true, textAlign = TextAlign.Start)
                        TableCell(
                            text = "₹${"%.2f".format(totalBalance)}",
                            modifier = Modifier.weight(1.6f),
                            textColor = if (totalBalance >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            isHeader = true,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailBadgeItem(label: String, typeText: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1565C0))
        Spacer(modifier = Modifier.height(2.dp))
        val badgeBg = if (typeText.contains("Group", ignoreCase = true)) Color(0xFFBBDEFB) else Color(0xFFE1BEE7)
        val badgeTxtColor = if (typeText.contains("Group", ignoreCase = true)) Color(0xFF0D47A1) else Color(0xFF4A148C)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = typeText,
                color = badgeTxtColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1565C0))
        Text(text = value, fontSize = 13.sp, color = Color(0xFF374151))
    }
}

@Composable
fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal,
    isHeader: Boolean = false,
    textAlign: TextAlign = TextAlign.Center
) {
    val finalColor = when {
        isHeader -> Color(0xFF002B49)
        textColor != Color.Unspecified -> textColor
        else -> Color.DarkGray
    }

    Box(
        modifier = modifier.padding(horizontal = 4.dp),
        contentAlignment = when (textAlign) {
            TextAlign.Start -> Alignment.CenterStart
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        Text(
            text = text,
            color = finalColor,
            textAlign = textAlign,
            fontSize = 13.sp,
            fontWeight = if (isHeader) FontWeight.Bold else fontWeight
        )
    }
}
