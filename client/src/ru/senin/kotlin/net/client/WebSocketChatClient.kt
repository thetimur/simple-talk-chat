package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.*
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


internal class OkHttpWebSocketHolder : WebSocket {
    private var webSocket: WebSocket? = null

    fun initiate(webSocket: WebSocket) {
        this.webSocket = webSocket
    }

    fun shutdown() {
        webSocket = null
    }

    override fun queueSize() = throw UnsupportedOperationException()

    override fun request() = throw UnsupportedOperationException()

    override fun send(text: String) = webSocket?.send(text) ?: false

    override fun send(bytes: ByteString) = webSocket?.send(bytes) ?: false

    override fun close(code: Int, reason: String?) = webSocket?.close(code, reason) ?: false

    override fun cancel() = webSocket?.cancel() ?: Unit
}

class WebSocketChatClient(host: String, port: Int) : ChatClient {

//    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
//        .readTimeout(0, TimeUnit.MILLISECONDS)
//        .build()
//
//    private val protocol = OkHttpWebSocket(
//        okHttpClient,
//        OkHttpWebSocket.SimpleRequestFactory(
//            { Request.Builder().url("ws://$host:$port").build() },
//            { ShutdownReason.GRACEFUL }
//        )
//    )
//
//    private val configuration = Scarlet.Configuration()
//
//    private val webApi : WebSocketApi = Scarlet(protocol, configuration).create()

    override fun sendMessage(message: Message)  {
        OkHttpWebSocketHolder().send(Gson().toJson(message))
    }
}
