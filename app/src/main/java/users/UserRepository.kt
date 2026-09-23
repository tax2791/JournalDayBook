package users

class UserRepository {

    suspend fun getUserDetails(username: String, token: String)
            = RetrofitInstance.api.getUserDetails(username, token)
}
