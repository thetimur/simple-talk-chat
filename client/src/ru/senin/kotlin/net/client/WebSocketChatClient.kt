package ru.senin.kotlin.net.client

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking

import ru.senin.kotlin.net.*

class WebSocketChatClient(private val host: String, private val port: Int) : ChatClient {

    private val client = HttpClient {
        install(io.ktor.client.features.websocket.WebSockets)
    }

    override fun sendMessage(message: Message)  {
        runBlocking {
            client.ws(
                method = HttpMethod.Get,
                host = host,
                port = port, path = "/v1/ws/message"
            ) {
                send(Frame.Text(Gson().toJson(message)))
            }
        }
    }
}
