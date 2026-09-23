package users

data class UserDetailsResponse(
    val status: String,
    val message: String,
    val user: UserDetails?
)

data class UserDetails(
    val messname: String?,
    val email: String?,
    val username: String?,
    val created_at: String?,
    val gstin: String?,
    val pan: String?,
    val address: String?,
    val mobile: String?,
    val other_details: String?,
    val verified: String?
)
