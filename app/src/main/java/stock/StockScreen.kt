package stock

//import androidx.compose.ui.graphics.Color
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import users.UserDetailsViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
    val username = sharedPref.getString("username", "") ?: ""
    val token = sharedPref.getString("sessionId", "") ?: ""

    // Auto-selected date (for "Add Stock")
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    // Date Filter States
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }

    // Backend list of item names
    val itemNames by viewModel.itemNames.collectAsState(emptyList())

    var expandedItem by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemName by remember { mutableStateOf("") }
    var availableQty by remember { mutableStateOf("") }

    var description by remember { mutableStateOf("") }
    var party_name by remember{mutableStateOf("") }
    var stockType by remember { mutableStateOf("Opening") }
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }

    val totalValue = remember(quantity, unitPrice) {
        val q = quantity.toDoubleOrNull() ?: 0.0
        val p = unitPrice.toDoubleOrNull() ?: 0.0
        (q * p).toString()
    }

    val stockList by viewModel.stockList.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch item names once
    LaunchedEffect(Unit) {
        viewModel.fetchItemNames(username)
    }

    // Reusable date picker function
    fun openDatePicker(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val cal = Calendar.getInstance()
                cal.set(y, m, d)
                onDateSelected(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time))
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock Management", fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {


            // ------------------------------- ADD STOCK SECTION -------------------------------
            item {
                // Your existing form fields — NO CHANGE
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDatePicker { date = it } },
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker { date = it } }) {
                            Icon(Icons.Default.DateRange, null)
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // STOCK TYPE DROPDOWN — unchanged
                val types = listOf("Opening", "Receipt", "Issue", "Closing")
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(
                        value = stockType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Stock Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        types.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                stockType = it
                                expanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Item dropdown (unchanged)
                Box {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            expandedItem = true
                        },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expandedItem && itemNames.isNotEmpty(),
                        onDismissRequest = { expandedItem = false },
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        val filteredItems = itemNames.filter {
                            it.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredItems.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No match — type manually") },
                                onClick = { expandedItem = false }
                            )
                        } else {
                            filteredItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        val regex = Regex("^(.*)\\(qty:(\\d+)\\)$")
                                        val match = regex.find(item.trim())

                                        if (match != null) {
                                            selectedItemName = match.groupValues[1].trim()
                                            availableQty = match.groupValues[2]
                                        } else {
                                            selectedItemName = item
                                            availableQty = ""
                                        }

                                        searchQuery = selectedItemName
                                        expandedItem = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (availableQty.isNotEmpty()) {
                    Text(
                        text = "Available Quantity: $availableQty",
                        color = Color(0xFF009688),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = party_name,
                    onValueChange = { party_name = it },
                    label = { Text("Party Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Unit Price") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalValue,
                    readOnly = true,
                    onValueChange = {},
                    label = { Text("Total Value") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val itemToSave =
                            if (selectedItemName.isNotEmpty()) selectedItemName else searchQuery

                        if (itemToSave.isEmpty() || quantity.isEmpty() || unitPrice.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Fill all fields") }
                        } else {
                            val stock = StockTransaction(
                                date = date,
                                item_name = itemToSave,
                                description = description,
                                party_name = party_name,
                                stock_type = stockType,
                                quantity = quantity,
                                unit_price = unitPrice,
                                total_value = totalValue,
                                username = username,
                                token = token
                            )
                            viewModel.submitStock(stock)

                            selectedItemName = ""
                            searchQuery = ""
                            description = ""
                            party_name = ""
                            stockType = "Opening"
                            quantity = ""
                            unitPrice = ""
                            availableQty = ""

                            scope.launch { snackbarHostState.showSnackbar("Stock saved!") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Stock", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                Text("Recent Transactions", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            // ------------------------------- FILTER SECTION -------------------------------
            item {
                Text("Filter by Date", fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fromDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openDatePicker { fromDate = it } },
                        trailingIcon = {
                            IconButton(
                                onClick = { openDatePicker { fromDate = it } }
                            ) { Icon(Icons.Default.DateRange, null) }
                        }
                    )

                    Spacer(Modifier.width(12.dp))

                    OutlinedTextField(
                        value = toDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openDatePicker { toDate = it } },
                        trailingIcon = {
                            IconButton(
                                onClick = { openDatePicker { toDate = it } }
                            ) { Icon(Icons.Default.DateRange, null) }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.fetchStockList(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Text("Apply Filter", color = Color.White)
                }

                Spacer(Modifier.height(20.dp))
            }
            // ------------------------------- FILTERED DATA DISPLAY -------------------------------

            val safeList = stockList ?: emptyList()

            // APPLY DATE FILTER
            val filteredList = safeList.filter { stock ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val stockDate = sdf.parse(stock.date)

                val from = if (fromDate.isNotEmpty()) sdf.parse(fromDate) else null
                val to = if (toDate.isNotEmpty()) sdf.parse(toDate) else null

                when {
                    from != null && to != null -> stockDate in from..to
                    from != null -> stockDate >= from
                    to != null -> stockDate <= to
                    else -> true
                }
            }

            items(filteredList.reversed()) { s ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${s.date} - ${s.item_name}", fontWeight = FontWeight.Bold)
                        Text("Invoice No.: ${s.invoice_no}")
                        Text("Party Name: ${s.party_name}")
                        Text("Type: ${s.stock_type}")
                        Text("Qty: ${s.quantity} × ${s.unit_price}")
                        Text("Value: ${s.total_value}", color = Color(0xFF009688))

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val samePartyList = stockList.filter { it.party_name == s.party_name && it.date == s.date }
                                generateInvoicePDF(context, samePartyList)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Print Bill", color = Color.White)
                        }
                    }
                }
            }

        }
    }
}


fun generateInvoicePDF(context: Context, items: List<StockTransaction>) {

    if (items.isEmpty()) return

    val pdf = PdfDocument()

    val borderPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
    }

    val titlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    // All items share same date & party
    val sample = items.first()

    // Calculate grand total
   // val grandTotal = items.sumOf { it.total_value.toFloatOrNull() ?: 0f }
    val grandTotal = items.sumOf { it.total_value.toDoubleOrNull() ?: 0.0 }


    // Create page
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    var y = 40
    val centerX = (595 / 2).toFloat()

    // ================= HEADER =================
    val viewModel = UserDetailsViewModel()

    CoroutineScope(Dispatchers.IO).launch {
        viewModel.loadUserDetailsSilently(context)
    }

    val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)

    val userName = prefs.getString("username", "Unknown") ?: "Unknown"
    val messName = prefs.getString("messname", "No Mess") ?: "No Mess"
    val address = prefs.getString("address", "No Address") ?: "No Address"
    val gstin = prefs.getString("gstin", "No GSTIN") ?: "No GSTIN"
    //val invoice_no = prefs.getString("invoice_no", "No Invoice") ?: "No Invoice"


    canvas.drawText("User: $userName", centerX, y.toFloat(), titlePaint)
    y += 40

    canvas.drawText("$messName", centerX, y.toFloat(), titlePaint)
    y += 40

//    canvas.drawText(sample.username, centerX, y.toFloat(), titlePaint)
//    y += 25

    textPaint.textSize = 10f
    canvas.drawText(address, centerX, y.toFloat(), textPaint)
    y += 14

    canvas.drawText("GSTIN: $gstin", centerX, y.toFloat(), textPaint)
    y += 25

    // TAX INVOICE
    titlePaint.textSize = 16f
    canvas.drawText("TAX INVOICE", centerX, y.toFloat(), titlePaint)
    y += 25

    val left = 25
    val right = 570

    // ================= PARTY BOX =================
    canvas.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + 90).toFloat(), borderPaint)
    textPaint.textSize = 12f

    canvas.drawText("PARTY DETAILS", (left + 10).toFloat(), (y + 20).toFloat(), textPaint)
    canvas.drawText(sample.party_name, (left + 10).toFloat(), (y + 40).toFloat(), textPaint)
    canvas.drawText("Address:", (left + 10).toFloat(), (y + 60).toFloat(), textPaint)
    canvas.drawText("GSTIN:", (left + 10).toFloat(), (y + 80).toFloat(), textPaint)

    y += 110

    // ================= INVOICE INFO =================
    canvas.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + 60).toFloat(), borderPaint)

    val col3 = 350
    val col4 = right

    canvas.drawLine(left.toFloat(), (y + 30).toFloat(), col4.toFloat(), (y + 30).toFloat(), borderPaint)

    canvas.drawText("INVOICE NO:", (left + 10).toFloat(), (y + 22).toFloat(), textPaint)
    canvas.drawText("DATE:", (col3 + 10).toFloat(), (y + 22).toFloat(), textPaint)

    //val invoiceNo = "INV-" + System.currentTimeMillis().toString().takeLast(5)

    canvas.drawText(sample.invoice_no ?: "", (left + 10).toFloat(), (y + 50).toFloat(), textPaint)
    canvas.drawText(sample.date, (col3 + 10).toFloat(), (y + 50).toFloat(), textPaint)

    y += 80

    // ================= TABLE HEADER =================
    val rowHeight = 30
    val tableTop = y

    canvas.drawRect(left.toFloat(), tableTop.toFloat(), right.toFloat(), (tableTop + rowHeight).toFloat(), borderPaint)

    val snCol = left + 10
    val descCol = left + 60
    val qtyCol = left + 330
    val rateCol = left + 380
    val amtCol = left + 450

    canvas.drawText("S.NO", snCol.toFloat(), (tableTop + 20).toFloat(), textPaint)
    canvas.drawText("DESCRIPTION", descCol.toFloat(), (tableTop + 20).toFloat(), textPaint)
    canvas.drawText("QTY", qtyCol.toFloat(), (tableTop + 20).toFloat(), textPaint)
    canvas.drawText("RATE", rateCol.toFloat(), (tableTop + 20).toFloat(), textPaint)
    canvas.drawText("AMOUNT", amtCol.toFloat(), (tableTop + 20).toFloat(), textPaint)

    y += rowHeight + 10

    // ================= MULTIPLE ITEM ROWS =================
    var serial = 1

    items.forEach { item ->

        canvas.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + rowHeight).toFloat(), borderPaint)

        canvas.drawText(serial.toString(), snCol.toFloat(), (y + 20).toFloat(), textPaint)
        canvas.drawText(item.description.ifEmpty { item.item_name }, descCol.toFloat(), (y + 20).toFloat(), textPaint)
        canvas.drawText(item.quantity, qtyCol.toFloat(), (y + 20).toFloat(), textPaint)
        canvas.drawText(item.unit_price, rateCol.toFloat(), (y + 20).toFloat(), textPaint)
        canvas.drawText(item.total_value, amtCol.toFloat(), (y + 20).toFloat(), textPaint)

        y += rowHeight
        serial++
    }

    y += 30

    // ================= TOTAL BOX =================
    canvas.drawRect(left.toFloat(), y.toFloat(), right.toFloat(), (y + 80).toFloat(), borderPaint)

    canvas.drawText("Grand Total", (left + 300).toFloat(), (y + 30).toFloat(), textPaint)
    canvas.drawText(grandTotal.toString(), (left + 450).toFloat(), (y + 30).toFloat(), textPaint)

    // Move down from total box
    y += 100

// ================= AUTHORISED SIGNATORY =================
    val signBoxTop = y
    val signBoxHeight = 70

// Draw a signature box on right side
    canvas.drawRect(
        350f,                   // left
        signBoxTop.toFloat(),  // top
        right.toFloat(),       // right
        (signBoxTop + signBoxHeight).toFloat(), // bottom
        borderPaint
    )

// Label text
    textPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(
        "Authorised Signatory",
        (350 + (right - 350) / 2).toFloat(),
        (signBoxTop + signBoxHeight - 15).toFloat(),
        textPaint
    )

    y += signBoxHeight + 20


    pdf.finishPage(page)

    // ================= SAVE FILE =================
    val file = File(
        context.getExternalFilesDir(null),
        "Invoice-${sample.party_name}-${sample.date}.pdf"
    )

    pdf.writeTo(FileOutputStream(file))
    pdf.close()

    // ================= OPEN FILE =================
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Install a PDF Viewer", Toast.LENGTH_LONG).show()
    }
}
//batch insert is not working