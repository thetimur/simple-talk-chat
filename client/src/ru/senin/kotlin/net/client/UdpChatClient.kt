package ru.senin.kotlin.net.client

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.cio.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

import ru.senin.kotlin.net.*
import java.net.InetSocketAddress

class UdpChatClient(private val host: String, private val port: Int) : ChatClient {

    override fun sendMessage(message: Message)  {
        runBlocking {
            aSocket(ActorSelectorManager(Dispatchers.IO))
                .udp()
                .connect(InetSocketAddress(host, port))
                .openWriteChannel(true)
                .write(Gson().toJson(message))
        }
    }
}
