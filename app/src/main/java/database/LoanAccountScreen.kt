package database

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kushal.mealapp.BannerAdView
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanAccountScreen(
    viewModel: MealViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val loans by viewModel.allLoans.collectAsState(initial = emptyList())
    val repayments by viewModel.allLoanRepayments.collectAsState(initial = emptyList())

    // Calculations
    val totalLoansGiven = remember(loans) { loans.sumOf { it.loanAmount } }
    val totalMoneyReceived = remember(repayments) { repayments.sumOf { it.amountReceived } }
    val netBalanceOutstanding = totalLoansGiven - totalMoneyReceived

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Loan Account & Collections", fontWeight = FontWeight.Bold)
                        Text(
                            "Track loans given & money received",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (loans.isEmpty() && repayments.isEmpty()) {
                                Toast.makeText(context, "No loan records available to export", Toast.LENGTH_SHORT).show()
                            } else {
                                LoanPdfExporter.exportLoanReportToPdf(context, loans, repayments)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Loan PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
        ) {
            // Summary Cards Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total Loan Given",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", totalLoansGiven)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text(
                            "Money Received",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", totalMoneyReceived)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Column {
                        Text(
                            "Unpaid Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", netBalanceOutstanding)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // Tab Navigation
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Party Ledgers") },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Give Loan") },
                    icon = { Icon(Icons.Default.Handshake, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Money Received") },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> PartyLedgerTab(
                        loans = loans,
                        repayments = repayments,
                        viewModel = viewModel
                    )

                    1 -> GiveLoanTab(
                        viewModel = viewModel,
                        existingParties = loans.map { it.partyName }.distinct(),
                        onLoanAdded = {
                            selectedTab = 0
                            Toast.makeText(context, "Loan Record Added! ✅", Toast.LENGTH_SHORT).show()
                        }
                    )

                    2 -> MoneyReceivedTab(
                        repayments = repayments,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 0: Party Ledgers (View Loans, Receive Money, Send Reminders, Edit, Delete)
// -----------------------------------------------------------------------------
data class PartyLoanSummary(
    val partyName: String,
    val phone: String,
    val totalLoanGiven: Double,
    val totalReceived: Double,
    val netBalance: Double,
    val partyLoans: List<LoanRecord>,
    val partyRepayments: List<LoanRepayment>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerTab(
    loans: List<LoanRecord>,
    repayments: List<LoanRepayment>,
    viewModel: MealViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPartyForRepayment by remember { mutableStateOf<PartyLoanSummary?>(null) }
    var selectedPartyForReminder by remember { mutableStateOf<PartyLoanSummary?>(null) }
    var selectedPartyForEdit by remember { mutableStateOf<PartyLoanSummary?>(null) }
    var selectedPartyForDelete by remember { mutableStateOf<PartyLoanSummary?>(null) }
    var selectedLoanForEdit by remember { mutableStateOf<LoanRecord?>(null) }

    // Grouping loans and repayments by Party Name
    val partySummaries = remember(loans, repayments) {
        val allParties = (loans.map { it.partyName } + repayments.map { it.partyName }).distinct()
        allParties.map { partyName ->
            val partyLoans = loans.filter { it.partyName.equals(partyName, ignoreCase = true) }
            val partyRepayments = repayments.filter { it.partyName.equals(partyName, ignoreCase = true) }
            val totalGiven = partyLoans.sumOf { it.loanAmount }
            val totalRec = partyRepayments.sumOf { it.amountReceived }
            val phone = partyLoans.firstOrNull { it.phone.isNotBlank() }?.phone ?: ""

            PartyLoanSummary(
                partyName = partyName,
                phone = phone,
                totalLoanGiven = totalGiven,
                totalReceived = totalRec,
                netBalance = totalGiven - totalRec,
                partyLoans = partyLoans,
                partyRepayments = partyRepayments
            )
        }.sortedByDescending { it.netBalance }
    }

    val filteredSummaries = remember(partySummaries, searchQuery) {
        if (searchQuery.isBlank()) partySummaries
        else partySummaries.filter { it.partyName.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search party / borrower...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No party found matching \"$searchQuery\"" else "No loan records added yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredSummaries) { party ->
                    PartyLedgerCard(
                        party = party,
                        onReceiveMoneyClick = { selectedPartyForRepayment = party },
                        onSendReminderClick = { selectedPartyForReminder = party },
                        onEditPartyClick = { selectedPartyForEdit = party },
                        onDeletePartyClick = { selectedPartyForDelete = party },
                        onEditLoanClick = { loan -> selectedLoanForEdit = loan },
                        onDeleteLoanClick = { loan -> viewModel.deleteLoan(loan) }
                    )
                }
            }
        }

        // Receive Money Modal Dialog
        if (selectedPartyForRepayment != null) {
            ReceiveMoneyDialog(
                partySummary = selectedPartyForRepayment!!,
                onDismiss = { selectedPartyForRepayment = null },
                onSubmit = { amount, date, mode, remarks ->
                    viewModel.addLoanRepayment(
                        LoanRepayment(
                            partyName = selectedPartyForRepayment!!.partyName,
                            amountReceived = amount,
                            paymentDate = date,
                            paymentMode = mode,
                            remarks = remarks
                        )
                    )
                    selectedPartyForRepayment = null
                    Toast.makeText(context, "Money Received Recorded! ✅", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Send Reminder Dialog
        if (selectedPartyForReminder != null) {
            SendReminderDialog(
                partySummary = selectedPartyForReminder!!,
                onDismiss = { selectedPartyForReminder = null }
            )
        }

        // Edit Party Dialog
        if (selectedPartyForEdit != null) {
            EditPartyDialog(
                party = selectedPartyForEdit!!,
                onDismiss = { selectedPartyForEdit = null },
                onSave = { newName, newPhone ->
                    viewModel.updatePartyLedger(selectedPartyForEdit!!.partyName, newName, newPhone)
                    selectedPartyForEdit = null
                    Toast.makeText(context, "Party Details Updated! ✅", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Party Confirmation Dialog
        if (selectedPartyForDelete != null) {
            DeletePartyConfirmationDialog(
                partyName = selectedPartyForDelete!!.partyName,
                onDismiss = { selectedPartyForDelete = null },
                onConfirmDelete = {
                    viewModel.deletePartyLedger(selectedPartyForDelete!!.partyName)
                    selectedPartyForDelete = null
                    Toast.makeText(context, "Party Ledger Deleted! 🗑️", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Loan Dialog
        if (selectedLoanForEdit != null) {
            EditLoanDialog(
                loan = selectedLoanForEdit!!,
                onDismiss = { selectedLoanForEdit = null },
                onSave = { updatedLoan ->
                    viewModel.updateLoan(updatedLoan)
                    selectedLoanForEdit = null
                    Toast.makeText(context, "Loan Record Updated! ✅", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun PartyLedgerCard(
    party: PartyLoanSummary,
    onReceiveMoneyClick: () -> Unit,
    onSendReminderClick: () -> Unit,
    onEditPartyClick: () -> Unit,
    onDeletePartyClick: () -> Unit,
    onEditLoanClick: (LoanRecord) -> Unit,
    onDeleteLoanClick: (LoanRecord) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = party.partyName.firstOrNull()?.uppercase() ?: "P",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = party.partyName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (party.phone.isNotBlank()) {
                            Text(
                                text = "📱 ${party.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Balance Status Badge & Action Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (party.netBalance > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = "Due: ₹${String.format(Locale.getDefault(), "%.0f", party.netBalance)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "SETTLED ✅",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEditPartyClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Party",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDeletePartyClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Party Ledger",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Given: ₹${String.format(Locale.getDefault(), "%.0f", party.totalLoanGiven)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Received: ₹${String.format(Locale.getDefault(), "%.0f", party.totalReceived)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReceiveMoneyClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receive Money", fontSize = 12.sp)
                }

                if (party.netBalance > 0) {
                    OutlinedButton(
                        onClick = onSendReminderClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Reminder", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand / Collapse Details Button
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isExpanded) "Hide Details ▲" else "View Details (${party.partyLoans.size} Loans) ▼")
            }

            // Expanded Breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Loan History:", style = MaterialTheme.typography.titleSmall)

                    party.partyLoans.forEach { loan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "₹${loan.loanAmount} on ${loan.loanDate}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                if (loan.remarks.isNotBlank()) {
                                    Text(
                                        text = loan.remarks,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEditLoanClick(loan) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Loan",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(onClick = { onDeleteLoanClick(loan) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Loan",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
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

// -----------------------------------------------------------------------------
// TAB 1: Give Loan Form
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiveLoanTab(
    viewModel: MealViewModel,
    existingParties: List<String>,
    onLoanAdded: () -> Unit
) {
    val context = LocalContext.current
    var partyName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loanAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var loanDate by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val todayFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    if (loanDate.isEmpty()) loanDate = todayFormatted

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            loanDate = "%04d-%02d-%02d".format(y, m + 1, d)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Give Loan to Party",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Record loan amount given to a borrower",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Party / Borrower Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) phone = it },
                    label = { Text("Mobile Number (For Reminders)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = loanAmount,
                    onValueChange = { loanAmount = it },
                    label = { Text("Loan Amount (₹) *") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = loanDate,
                    onValueChange = {},
                    label = { Text("Loan Date *") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Terms (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val amount = loanAmount.toDoubleOrNull() ?: 0.0
                        if (partyName.isBlank()) {
                            Toast.makeText(context, "Please enter party / borrower name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amount <= 0) {
                            Toast.makeText(context, "Please enter a valid loan amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.addLoan(
                            LoanRecord(
                                partyName = partyName.trim(),
                                loanAmount = amount,
                                interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                                loanDate = loanDate,
                                remarks = remarks,
                                phone = phone.trim()
                            )
                        )
                        onLoanAdded()
                    },
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Loan Record",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: Money Received Log (Repayments History)
// -----------------------------------------------------------------------------
@Composable
fun MoneyReceivedTab(
    repayments: List<LoanRepayment>,
    viewModel: MealViewModel
) {
    val context = LocalContext.current
    var selectedRepaymentForEdit by remember { mutableStateOf<LoanRepayment?>(null) }

    if (repayments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No loan payments received yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(repayments) { item ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.partyName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Received: ₹${item.amountReceived} • ${item.paymentDate} (${item.paymentMode})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32)
                            )
                            if (item.remarks.isNotBlank()) {
                                Text(
                                    text = item.remarks,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedRepaymentForEdit = item }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Repayment",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = { viewModel.deleteLoanRepayment(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedRepaymentForEdit != null) {
            EditRepaymentDialog(
                repayment = selectedRepaymentForEdit!!,
                onDismiss = { selectedRepaymentForEdit = null },
                onSave = { updatedRepayment ->
                    viewModel.updateLoanRepayment(updatedRepayment)
                    selectedRepaymentForEdit = null
                    Toast.makeText(context, "Payment Record Updated! ✅", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// MODAL DIALOGS: Edit Party, Delete Party, Edit Loan, Edit Repayment
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPartyDialog(
    party: PartyLoanSummary,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhone: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(party.partyName) }
    var phoneInput by remember { mutableStateOf(party.phone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Party / Borrower") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Party Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneInput = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput.trim(), phoneInput.trim())
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeletePartyConfirmationDialog(
    partyName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Party Ledger") },
        text = {
            Text("Are you sure you want to delete '$partyName' and ALL associated loan records and payments? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete All Records")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanDialog(
    loan: LoanRecord,
    onDismiss: () -> Unit,
    onSave: (LoanRecord) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(loan.loanAmount.toString()) }
    var dateText by remember { mutableStateOf(loan.loanDate) }
    var phoneInput by remember { mutableStateOf(loan.phone) }
    var remarksInput by remember { mutableStateOf(loan.remarks) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> dateText = "%04d-%02d-%02d".format(y, m + 1, d) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Loan Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Borrower: ${loan.partyName}", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Loan Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Loan Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneInput = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(
                            loan.copy(
                                loanAmount = amt,
                                loanDate = dateText,
                                phone = phoneInput.trim(),
                                remarks = remarksInput.trim()
                            )
                        )
                    } else {
                        Toast.makeText(context, "Enter a valid loan amount", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRepaymentDialog(
    repayment: LoanRepayment,
    onDismiss: () -> Unit,
    onSave: (LoanRepayment) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(repayment.amountReceived.toString()) }
    var dateText by remember { mutableStateOf(repayment.paymentDate) }
    var modeText by remember { mutableStateOf(repayment.paymentMode) }
    var remarksInput by remember { mutableStateOf(repayment.remarks) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> dateText = "%04d-%02d-%02d".format(y, m + 1, d) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Money Received") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Party: ${repayment.partyName}", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Payment Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                )

                OutlinedTextField(
                    value = modeText,
                    onValueChange = { modeText = it },
                    label = { Text("Payment Mode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(
                            repayment.copy(
                                amountReceived = amt,
                                paymentDate = dateText,
                                paymentMode = modeText.trim(),
                                remarks = remarksInput.trim()
                            )
                        )
                    } else {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -----------------------------------------------------------------------------
// MODAL DIALOGS: Receive Money & Send Reminder
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveMoneyDialog(
    partySummary: PartyLoanSummary,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, date: String, mode: String, remarks: String) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var remarks by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> paymentDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Money against Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Party: ${partySummary.partyName}", fontWeight = FontWeight.Bold)
                Text("Outstanding Balance: ₹${partySummary.netBalance}", color = Color(0xFFD32F2F))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = {},
                    label = { Text("Payment Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                )

                OutlinedTextField(
                    value = paymentMode,
                    onValueChange = { paymentMode = it },
                    label = { Text("Payment Mode (Cash/UPI/Bank)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onSubmit(amount, paymentDate, paymentMode, remarks)
                    } else {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SendReminderDialog(
    partySummary: PartyLoanSummary,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf(partySummary.phone) }

    val message = "Dear ${partySummary.partyName}, gentle reminder from Meal App: Your outstanding loan balance is ₹${String.format(Locale.getDefault(), "%.2f", partySummary.netBalance)}. Kindly settle at your earliest convenience. Thank you!"

    fun sendWhatsAppReminder() {
        val rawPhone = phoneInput.trim()
        val formattedPhone = if (rawPhone.length == 10) "91$rawPhone" else rawPhone
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun sendSmsReminder() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("smsto:${phoneInput.trim()}")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "SMS app not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Loan Reminder 📱") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Remind ${partySummary.partyName} for outstanding ₹${partySummary.netBalance}")

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) phoneInput = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Message Preview:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { sendWhatsAppReminder() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send via WhatsApp 💬", color = Color.White)
                }

                OutlinedButton(
                    onClick = { sendSmsReminder() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send via SMS 📱")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", textAlign = TextAlign.Center)
            }
        }
    )
}
