package journal

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("save_transaction.php")
    suspend fun saveTransaction(
        @Body transaction: Transaction
    ): ApiResponse



    @POST("save_transaction.php?action=fetch")
    suspend fun getAllTransactions(@Body credentials: Map<String, String>): TransactionResponse

}