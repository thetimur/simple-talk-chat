package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.HttpApi
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress

class HttpChatClient(host: String, port: Int) : ChatClient {
    private val httpApi: HttpApi = Retrofit.Builder()
            .baseUrl(UserAddress(Protocol.HTTP, host, port).toString())
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(HttpApi::class.java)

    override fun sendMessage(message: Message) {
        val response = httpApi.sendMessage(message).execute()
        if (!response.isSuccessful) {
            println("${response.code()} ${response.message()}}")
        }
    }
}
