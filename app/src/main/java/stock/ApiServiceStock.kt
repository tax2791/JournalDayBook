package stock

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiServiceStock {

    @POST("save_stock.php")
    suspend fun saveStock(@Body stock: StockTransaction): Response<StockApiResponse>

    @GET("get_stock_list.php")
    suspend fun getAllStock(
        @Query("username") username: String,
        @Query("token") token: String
    ): Response<StockApiResponse>


    @GET("getStockList.php")
    suspend fun getItemNames(@Query("username") username: String): Response<ItemListResponse>


}
