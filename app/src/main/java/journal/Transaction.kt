package journal

data class Transaction(
    val date: String,
    val party_name: String,
    val description: String,
    val amount: String,
    val type: String,        // "Debit" or "Credit"
    val category: String,    // e.g. "Expenses", "Income"
    val username: String,    // ✅ Logged-in username
    val token: String        // ✅ Login token for verification
)

data class ApiResponse(
    val status: String,
    val message: String,
    val transactions: List<Transaction> = emptyList()
)

data class TransactionResponse(
    val status: String,
    val transactions: List<Transaction>
)
