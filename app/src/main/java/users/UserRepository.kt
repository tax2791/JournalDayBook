package users

class UserRepository {

    suspend fun getUserDetailsGet(username: String, token: String, email: String? = null) =
        RetrofitInstance.api.getUserDetailsGet(username, token, email)

    suspend fun getUserDetailsPost(username: String, token: String, email: String? = null) =
        RetrofitInstance.api.getUserDetailsPost(username, token, email)
}
