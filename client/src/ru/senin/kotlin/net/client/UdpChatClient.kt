package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

import ru.senin.kotlin.net.*
import java.net.InetSocketAddress
import java.net.SocketException
import java.rmi.AlreadyBoundException

class UdpChatClient(private val host: String, private val port: Int) : ChatClient {

    private val objectMapper = jacksonObjectMapper()
    private val UDP_TRIES: Int = 256

    override fun sendMessage(message: Message)  {
        runBlocking {
            var counter = 0
            var sent = false
            var exception = Throwable("Message not sent")
            while (counter < UDP_TRIES) {
                try {
                    counter++
                    aSocket(ActorSelectorManager(Dispatchers.IO))
                        .udp()
                        .connect(InetSocketAddress(host, port))
                        .openWriteChannel(true)
                        .write(objectMapper.writeValueAsString(message))
                        sent = true
                        break
                } catch (e : Throwable) {
                    exception = e
                    continue
                }
            }
            if (!sent) {
                throw exception
            }
        }
    }
}
