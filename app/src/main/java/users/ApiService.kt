package users

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("get_user_details.php")
    suspend fun getUserDetailsGet(
        @Query("username") username: String,
        @Query("token") token: String,
        @Query("email") email: String? = null
    ): Response<UserDetailsResponse>

    @FormUrlEncoded
    @POST("get_user_details.php")
    suspend fun getUserDetailsPost(
        @Field("username") username: String,
        @Field("token") token: String,
        @Field("email") email: String? = null
    ): Response<UserDetailsResponse>
}
