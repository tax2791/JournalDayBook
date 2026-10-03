package com.kushal.mealapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar

class NameActivityAPI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScreen1()
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun AppScreen1() {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val sharedPrefs = remember { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }

    // Observe session
    var sessionToken by remember {
        mutableStateOf(sharedPrefs.getString("sessionId", null) ?: sharedPrefs.getString("sessionId1", null))
    }

    // Redirect to login if no token
    LaunchedEffect(sessionToken) {
        if (sessionToken.isNullOrEmpty()) {
            val intent = Intent(context, LoginComposeActivity::class.java)
            intent.putExtra("redirectActivity", "NameActivityAPI")
            context.startActivity(intent)
            activity?.finish()
        }
    }

    // If logged in, show main content
    if (!sessionToken.isNullOrEmpty()) {
        NameScreen1(sessionToken!!) {
            sharedPrefs.edit {
                remove("sessionId")
                remove("username")
            }
            sessionToken = null
            val intent = Intent(context, LoginComposeActivity::class.java)
            intent.putExtra("redirectActivity", "NameActivityAPI")
            context.startActivity(intent)
            activity?.finish()
        }
    } else {
        // Loading UI
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Redirecting to Login...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameScreen1(sessionToken: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val storedUsername = sharedPrefs.getString("username", "User") ?: "User"

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // State variables
    var name by remember { mutableStateOf("") }
    var joinDate by remember { mutableStateOf("") }
    var isLoadingNames by remember { mutableStateOf(false) }
    var isSubmittingName by remember { mutableStateOf(false) }
    var isExitingMember by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val namesList = remember { mutableStateListOf<NameRecord>() }
    var selectedMember by remember { mutableStateOf<NameRecord?>(null) }
    var exitDate by remember { mutableStateOf("") }

    val monthlyStats = remember { mutableStateOf<Map<YearMonth, Int>>(emptyMap()) }

    // Date Pickers Setup
    val calendar = Calendar.getInstance()
    val todayDateFormatted = "%02d-%02d-%d".format(
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR)
    )

    if (joinDate.isEmpty()) {
        joinDate = todayDateFormatted
    }

    val joinDatePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            joinDate = "%02d-%02d-%d".format(d, m + 1, y)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Load names on initial launch
    LaunchedEffect(Unit) {
        if (storedUsername.isNotEmpty()) {
            isLoadingNames = true
            retrieveNames2(
                sessionToken = sessionToken,
                username = storedUsername,
                onSuccess = { records ->
                    namesList.clear()
                    namesList.addAll(records)
                    monthlyStats.value = calculateMonthlyActiveMembers1(records)
                    isLoadingNames = false
                },
                onError = { err ->
                    isLoadingNames = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(err)
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Member Directory",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "User: $storedUsername",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, HomeActivity::class.java)
                        context.startActivity(intent)
                        activity?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navigation Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Members") },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Add Member") },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Monthly Stats") },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> MembersListTab(
                        namesList = namesList,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        isLoading = isLoadingNames,
                        selectedMember = selectedMember,
                        onSelectMember = { member ->
                            selectedMember = if (selectedMember?.name == member.name) null else member
                            exitDate = ""
                        },
                        exitDate = exitDate,
                        onSelectExitDate = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    exitDate = "%02d-%02d-%04d".format(d, m + 1, y)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        isExiting = isExitingMember,
                        onSubmitExit = {
                            val member = selectedMember ?: return@MembersListTab
                            if (exitDate.isEmpty()) return@MembersListTab

                            isExitingMember = true
                            coroutineScope.launch {
                                exitMember(
                                    token = sessionToken,
                                    name = member.name,
                                    exitDate = exitDate,
                                    onSuccess = { msg ->
                                        isExitingMember = false
                                        selectedMember = null
                                        exitDate = ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                        // Refresh list
                                        coroutineScope.launch {
                                            retrieveNames2(
                                                sessionToken,
                                                storedUsername,
                                                onSuccess = { list ->
                                                    namesList.clear()
                                                    namesList.addAll(list)
                                                    monthlyStats.value = calculateMonthlyActiveMembers1(list)
                                                },
                                                onError = {}
                                            )
                                        }
                                    },
                                    onError = { err ->
                                        isExitingMember = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(err)
                                        }
                                    }
                                )
                            }
                        },
                        onRefresh = {
                            isLoadingNames = true
                            coroutineScope.launch {
                                retrieveNames2(
                                    sessionToken = sessionToken,
                                    username = storedUsername,
                                    onSuccess = { records ->
                                        namesList.clear()
                                        namesList.addAll(records)
                                        monthlyStats.value = calculateMonthlyActiveMembers1(records)
                                        isLoadingNames = false
                                    },
                                    onError = { err ->
                                        isLoadingNames = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(err)
                                        }
                                    }
                                )
                            }
                        }
                    )

                    1 -> AddMemberTab(
                        name = name,
                        onNameChange = { name = it },
                        joinDate = joinDate,
                        onOpenDatePicker = { joinDatePickerDialog.show() },
                        isSubmitting = isSubmittingName,
                        onSubmit = {
                            if (name.isBlank()) return@AddMemberTab
                            isSubmittingName = true
                            coroutineScope.launch {
                                insertData1(
                                    token = sessionToken,
                                    name = name,
                                    joinDate = joinDate,
                                    onSuccess = { msg ->
                                        isSubmittingName = false
                                        name = ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                        // Switch to members tab & refresh
                                        selectedTab = 0
                                        coroutineScope.launch {
                                            retrieveNames2(
                                                sessionToken,
                                                storedUsername,
                                                onSuccess = { list ->
                                                    namesList.clear()
                                                    namesList.addAll(list)
                                                    monthlyStats.value = calculateMonthlyActiveMembers1(list)
                                                },
                                                onError = {}
                                            )
                                        }
                                    },
                                    onError = { err ->
                                        isSubmittingName = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(err)
                                        }
                                    }
                                )
                            }
                        }
                    )

                    2 -> MonthlyStatsTab(monthlyStats = monthlyStats.value)
                }
            }
        }
    }
}

// -----------------------------
// TAB 0: Members List & Exit
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersListTab(
    namesList: List<NameRecord>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isLoading: Boolean,
    selectedMember: NameRecord?,
    onSelectMember: (NameRecord) -> Unit,
    exitDate: String,
    onSelectExitDate: () -> Unit,
    isExiting: Boolean,
    onSubmitExit: () -> Unit,
    onRefresh: () -> Unit
) {
    val filteredList = remember(namesList, searchQuery) {
        if (searchQuery.isBlank()) namesList
        else namesList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val activeCount = remember(namesList) {
        namesList.count { it.exitDate.isNullOrBlank() || it.exitDate == "0000-00-00" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Card + Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search members...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Active: $activeCount") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Total: ${namesList.size}") }
                )
            }

            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Member Exit Action Card
        AnimatedVisibility(visible = selectedMember != null) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Exit Member: ${selectedMember?.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(
                            onClick = { onSelectMember(selectedMember!!) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onSelectExitDate,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (exitDate.isEmpty()) "Select Exit Date" else exitDate)
                        }

                        Button(
                            onClick = onSubmitExit,
                            enabled = exitDate.isNotEmpty() && !isExiting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isExiting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onError,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }

        // List of Members
        if (isLoading && filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No members found matching \"$searchQuery\"" else "No members added yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList) { item ->
                    val isActive = item.exitDate.isNullOrBlank() || item.exitDate == "0000-00-00"
                    val isSelected = selectedMember?.name == item.name

                    OutlinedCard(
                        onClick = {
                            if (isActive) onSelectMember(item)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                !isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = item.name.firstOrNull()?.uppercase() ?: "?",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Joined: ${item.joinDate ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isActive) {
                                AssistChip(
                                    onClick = { onSelectMember(item) },
                                    label = { Text("Active") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Exited: ${item.exitDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------
// TAB 1: Add Member Form
// -----------------------------
@Composable
fun AddMemberTab(
    name: String,
    onNameChange: (String) -> Unit,
    joinDate: String,
    onOpenDatePicker: () -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add New Member",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Register a new member to manage meal accounts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Member Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = joinDate,
                    onValueChange = {},
                    label = { Text("Joining Date") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onOpenDatePicker) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDatePicker() }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onSubmit,
                    enabled = name.isNotBlank() && !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Member", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// -----------------------------
// TAB 2: Monthly Active Stats
// -----------------------------
@Composable
fun MonthlyStatsTab(monthlyStats: Map<YearMonth, Int>) {
    if (monthlyStats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No member history available to calculate statistics.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Monthly Statistics",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Active members tracked by month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(monthlyStats.entries.toList()) { (month, count) ->
                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$count Active",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------
// Retrofit API functions
// -----------------------------
suspend fun insertData1(
    token: String,
    name: String,
    joinDate: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val response = RetrofitClient.api.insertData(
            token = "Bearer $token",
            body = InsertRequest(name, joinDate, token)
        )

        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) onSuccess(body.message)
            else onError(body?.message ?: "Unknown error")
        } else onError("Server error: ${response.code()}")
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Network error")
    }
}

suspend fun retrieveNames2(
    sessionToken: String,
    username: String,
    onSuccess: (List<NameRecord>) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val response = RetrofitClient.api.getNames1(
            token = "Bearer $sessionToken",
            username = username
        )

        Log.d("API_CALL", "Response code: ${response.code()}")
        Log.d("API_CALL", "Response body: ${response.body()}")

        if (response.isSuccessful) {
            val body = response.body()
            Log.d("API_CALL", "Success: ${body?.success}")
            Log.d("API_CALL", "Message: ${body?.message}")
            Log.d("API_CALL", "Data: ${body?.data}")

            if (body?.success == true && body.data != null) {
                onSuccess(body.data)
            } else {
                onError(body?.message ?: "No data found")
            }
        } else onError("HTTP ${response.code()}")
    } catch (e: Exception) {
        Log.e("API_ERROR", "Exception: ${e.localizedMessage}", e)
        onError(e.localizedMessage ?: "Network error")
    }
}

fun calculateMonthlyActiveMembers1(records: List<NameRecord>): Map<YearMonth, Int> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Safely parse only valid records
    val parsedRecords = records.mapNotNull { record ->
        val joinDate = record.joinDate
            ?.takeIf { it.isNotBlank() && it != "0000-00-00" }
            ?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }
            ?: return@mapNotNull null // skip records with no valid join date

        val exitDate = record.exitDate
            ?.takeIf { it.isNotBlank() && it != "0000-00-00" }
            ?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }

        Triple(record.name, joinDate, exitDate)
    }

    if (parsedRecords.isEmpty()) return emptyMap()

    // Determine range of months to evaluate
    val startMonth = YearMonth.from(parsedRecords.minOf { it.second })
    val endMonth = YearMonth.from(parsedRecords.maxOf { it.third ?: LocalDate.now() })

    val result = mutableMapOf<YearMonth, Int>()
    var currentMonth = startMonth

    // Count active members month by month
    while (!currentMonth.isAfter(endMonth)) {
        val activeCount = parsedRecords.count { (_, join, exit) ->
            val exitMonth = exit?.let { YearMonth.from(it) }
            currentMonth >= YearMonth.from(join) &&
                    (exitMonth == null || currentMonth <= exitMonth)
        }
        result[currentMonth] = activeCount
        currentMonth = currentMonth.plusMonths(1)
    }

    return result
}

suspend fun exitMember(
    token: String,
    name: String,
    exitDate: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val response = RetrofitClient.api.exitMember(
            token = "Bearer $token",
            body = ExitRequest(name, exitDate, token)
        )

        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) {
                onSuccess(body.message)
            } else {
                onError(body?.message ?: "Unknown error")
            }
        } else {
            onError("Server error: ${response.code()}")
        }

    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Network error")
    }
}
