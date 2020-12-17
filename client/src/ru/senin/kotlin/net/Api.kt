package ru.senin.kotlin.net

import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow
import okhttp3.WebSocket
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface RegistryApi {
    @POST("/v1/users")
    fun register(@Body newUserInfo: UserInfo): Call<Map<String, String>>

    @PUT("/v1/users/{user}")
    fun update(@Body address: UserAddress): Call<Map<String, String>>

    @GET("/v1/users")
    fun list(): Call<Map<String, UserAddress>>

    @DELETE("/v1/users/{user}")
    fun unregister(@Path("user") user: String): Call<Map<String, String>>
}

interface HttpApi {
    @POST("/v1/message")
    fun sendMessage(@Body message: Message): Call<Map<String, String>>
}

interface WebSocketApi {
    @Send
    fun sendMessage(message: Message)
}

fun <T> Response<T>.getOrNull() : T? {
    if (!this.isSuccessful) {
        return null
    }
    return this.body()
}