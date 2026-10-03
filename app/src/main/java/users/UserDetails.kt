package users

import com.google.gson.annotations.SerializedName

data class UserDetailsResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: UserDetails? = null,
    @SerializedName("data") val data: UserDetails? = null
)

data class UserDetails(
    @SerializedName("messname") val messname: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("gstin") val gstin: String? = null,
    @SerializedName("pan") val pan: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("other_details") val other_details: String? = null,
    @SerializedName("verified") val verified: String? = null
)
