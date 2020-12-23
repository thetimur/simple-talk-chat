package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

import ru.senin.kotlin.net.*

class WebSocketChatClient(private val host: String, private val port: Int) : ChatClient {

    private val objectMapper = jacksonObjectMapper()
    private val client = HttpClient(CIO).config { install(WebSockets) }
    private val messagesChannel = Channel<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val job = scope.launch {
        client.ws(HttpMethod.Get, host, port, "/v1/ws/message") {
            while (isActive) {
                val content = messagesChannel.receive()
                val data = Frame.Text(content)
                send(data)
            }
        }
    }

    override fun sendMessage(message: Message)  {
            if (!job.isActive) {
                throw IllegalStateException("Client already closed")
            }
            scope.launch {
                try {
                    messagesChannel.send(objectMapper.writeValueAsString(message))
                } catch (e : Throwable) {
                    e.printStackTrace()
                }
            }
    }
}