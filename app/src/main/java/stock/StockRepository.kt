package stock

import retrofit2.Response

class StockRepository {

    suspend fun submitStock(stock: StockTransaction): Response<StockApiResponse> {
        return RetrofitStock.api.saveStock(stock)
    }
    suspend fun getStockList(username: String, token: String): Response<StockApiResponse> {
        return RetrofitStock.api.getAllStock(username, token)
    }

    suspend fun getItemNames(username: String): Response<ItemListResponse> {
        return RetrofitStock.api.getItemNames(username)
    }

}
