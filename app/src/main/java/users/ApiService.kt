package users

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("get_user_details.php")
    suspend fun getUserDetails(
        @Query("username") username: String,
        @Query("token") token: String
    ): Response<UserDetailsResponse>
}
