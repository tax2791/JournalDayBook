package stock


data class StockTransaction(
    val date: String,
    val item_name: String,
    val description: String,
    val party_name: String,
    val stock_type: String,   // Opening, Receipt, Issue, Closing
    val quantity: String,
    val unit_price: String,
    val total_value: String,
    val username: String,
    val token: String,
    val invoice_no: String? = null
)

data class StockApiResponse(
    val status: String?,
    val message: String?,
    val stockTransaction: List<StockTransaction> = emptyList()
)
data class ItemListResponse(
    val status: String,
    val items: List<ItemData>
)
data class ItemData(
    val item_name: String,
    val quantity: Int
)