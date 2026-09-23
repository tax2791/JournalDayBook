package stock

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitStock {
    private const val BASE_URL = "https://legalcount.in/journal/" // Change this to your domain

    val api: ApiServiceStock by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiServiceStock::class.java)
    }
}
