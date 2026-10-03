package com.kushal.mealapp

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


// -------------------- LOGIN --------------------
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String,
    val username: String?,
    val sessionId1: String? // PHP session ID
)

// -------------------- MEMBER NAMES --------------------
data class MemberNamesResponse(
    val success: Boolean,
    val names: List<String>,
    val username: String?    // Logged-in user
)

data class ExitRequest(
    val name: String,
    val exitDate: String,
    val token: String
)

// -------------------- GENERAL API RESPONSE --------------------
data class ApiResponse(
    val success: Boolean,
    val status: String,
    val message: String
)

// -------------------- CHAT --------------------
data class ChatItem(
    val id: Int,
    val date: String,
    val text: String,
    val username: String,
    val created_at: String?,
    val tags: String?    // ✅ NEW

)

data class ChatResponse(
    val success: Boolean,
    val status: String,
    val message: String
)

data class NameRecordResponse(
    val name: String,
    val join_dt: String,
    val exit_dt: String
)

data class GetNamesResponse(
    val success: Boolean,
    val message: String,
    val data: List<NameRecordResponse>
)

//data class NameRecord(
//    val name: String,
//    val joinDate: String,
//    val exitDate: String
//)

data class NameRecord(
    @SerializedName("name") val name: String,
    @SerializedName("join_dt") val joinDate: String?,
    @SerializedName("exit_dt") val exitDate: String?
)



data class InsertRequest(
    val name: String,
    val join_dt: String,
    val token: String
)

data class InsertResponse(
    val success: Boolean,
    val message: String
)

data class RetrieveResponse(
    val success: Boolean,
    val message: String?,
    val data: List<NameRecord>?
)

data class ChatListResponse(
    val success: Boolean,
    val data: List<ChatItem>,
    val message: String? = null
)

//Password Reset

data class ForgotPasswordResponse(
    val status: String,
    val message: String
)

data class ResetPasswordResponse(
    val status: String,
    val message: String
)

data class ItemRecord(val name: String)
data class ApiResponse1(val success: Boolean, val message: String)

// -------------------- API INTERFACE --------------------
interface ApiService {

    // ----- LOGIN ENDPOINTS -----
    @Headers("Content-Type: application/json")
    @POST("login1.php")
    fun loginUserWithJson(
        @Body loginRequest: LoginRequest
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("login1.php")
    fun loginUserWithForm(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // ----- MEAL SUBMISSION -----
    @FormUrlEncoded
    @POST("mealupdateapp.php")
    fun submitForm(
        @Field("name") name: String,
        @Field("item") item: String,
        @Field("meal") meal: String,
        @Field("expenditure") expenditure: String,
        @Field("price") price: String,
        @Field("date") date: String,
        @Field("depositTo") depositTo: String, // ✅ NEW FIELD
        @Field("token") token: String
    ): Call<JSONObject>

    @GET("getnames.php")
    fun getMemberNames(
        @Query("username") username: String
    ): Call<MemberNamesResponse>

    @FormUrlEncoded
    @POST("insert_data.php")
    fun insertName(
        @Field("name") name: String,
        @Header("Authorization") token: String
    ): Call<ApiResponse>

    // -------------------- CHAT ENDPOINTS --------------------

    // Get all chats
    @GET("chat_api.php")
    suspend fun getChats(@Query("username") username: String): Response<ChatListResponse>

    // Add chat
    @FormUrlEncoded
    @POST("chat_api.php")
    suspend fun addChat(
        @Field("action") action: String = "add",
        @Field("date") date: String,
        @Field("text") text: String,
        @Field("tags") tags: String,
        @Field("username")
        username: String
    ): Response<ChatResponse>

    // Update chat
    @FormUrlEncoded
    @POST("chat_api.php")
    suspend fun updateChat(
        @Field("action") action: String = "update",
        @Field("id") id: Int,
        @Field("text") text: String,
        @Field("username") username: String
    ): Response<ChatResponse>

    // Delete chat
    @FormUrlEncoded
    @POST("chat_api.php")
    suspend fun deleteChat(
        @Field("action") action: String = "delete",
        @Field("id") id: Int,
        @Field("username") username: String
    ): Response<ChatResponse>


    @GET("getnameapp.php")
    suspend fun getNames(
        @Header("Authorization") token: String,
        @Query("username") username: String
    ): Response<GetNamesResponse>



    @POST("insert_data.php")
    suspend fun insertData(
        @Header("Authorization") token: String,
        @Body body: InsertRequest
    ): Response<InsertResponse>

    @POST("exit_member.php")
    suspend fun exitMember(
        @Header("Authorization") token: String,
        @Body body: ExitRequest
    ): Response<ApiResponse>


    @GET("getnameapp.php")
    suspend fun getNames1(
        @Header("Authorization") token: String,
        @Query("username") username: String
    ): Response<RetrieveResponse>


    @FormUrlEncoded
    @POST("send_otp.php")
    suspend fun sendOtp(
        @Field("username") username: String,
        @Field("email") email: String
    ): Response<ForgotPasswordResponse>

    @FormUrlEncoded
    @POST("reset_password.php")
    suspend fun resetPassword(
        @Field("username") username: String,
        @Field("email") email: String,
        @Field("otp") otp: String,
        @Field("password") newPassword: String
    ): Response<ResetPasswordResponse>



    @GET("get_items.php")
    suspend fun getItems(@Query("username") username: String): Response<List<ItemRecord>>

    @FormUrlEncoded
    @POST("insert_item.php")
    suspend fun insertItem(
        @Field("username") username: String,
        @Field("item_name") itemName: String
    ): Response<ApiResponse1>

}


