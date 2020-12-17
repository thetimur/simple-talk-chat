package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.websocket.*
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.*
import ru.senin.kotlin.net.server.HttpChatServer
import ru.senin.kotlin.net.server.UdpChatServer
import ru.senin.kotlin.net.server.WebSocketChatServer
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ClientTest {

    private val objectMapper = jacksonObjectMapper()
    private val registry: RegistryApi = Retrofit.Builder()
        .baseUrl("http://localhost:8088")
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .build().create(RegistryApi::class.java)

    private fun addUser (name: String, host: String, port: Int, protocol: Protocol): List<Any> {

        val server = when(protocol) {
            Protocol.HTTP -> HttpChatServer(host, port)
            Protocol.WEBSOCKET -> WebSocketChatServer(host, port)
            Protocol.UDP -> UdpChatServer(host, port)
            else -> UdpChatServer(host, port)
        }

        val chat = Chat(name, registry)
        server.setMessageListener(chat)

        val serverJob = thread {
            server.start()
        }

        registry.register(UserInfo(name, UserAddress(protocol, host, port))).execute()

        return listOf (chat, server, serverJob)
    }

    fun deleteUser (name: String, server: Any, serverJob: Thread, protocol: Protocol) {

        registry.unregister(name).execute()

        when(protocol){

            Protocol.HTTP -> (server as HttpChatServer).stop()
            Protocol.WEBSOCKET -> (server as WebSocketChatServer).stop()
            Protocol.UDP -> (server as UdpChatServer).stop()
            else -> (server as UdpChatServer).stop()
        }

        serverJob.join()

    }

    @Test
    fun testClientHTTP () {

        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = 8081
        val (chatUser1, serverUser1, serverJobUser1) = addUser(nameUser1, host, portUser1, Protocol.HTTP)

        val nameUser2 = "User2"
        val portUser2 = 8082
        val (chatUser2, serverUser2, serverJobUser2) = addUser(nameUser2, host, portUser2, Protocol.HTTP)
        (chatUser1 as Chat).updateUsers()
        (chatUser2 as Chat).updateUsers()

        val sentMessageUser1 = mutableListOf<String>()
        val sentMessageUser2 = mutableListOf<String>()

        for (i in 1..3) {

            val messageUser1 = "abc$i"
            sentMessageUser1.add(messageUser1)
            chatUser1.testMessageSent(nameUser2, messageUser1)

            val messageUser2 = ":)$i"
            sentMessageUser2.add(messageUser2)
            chatUser2.testMessageSent(nameUser1, messageUser2)

        }

        assertEquals(sentMessageUser1, chatUser2.allMessages)
        assertEquals(sentMessageUser2, chatUser1.allMessages)

        deleteUser(nameUser1, serverUser1 as HttpChatServer, serverJobUser1 as Thread, Protocol.HTTP)
        deleteUser(nameUser2, serverUser2 as HttpChatServer, serverJobUser2 as Thread, Protocol.HTTP)

    }

    @Test
    fun testClientWebSocket () {

        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = 8083
        val (chatUser1, serverUser1, serverJobUser1) = addUser(nameUser1, host, portUser1, Protocol.WEBSOCKET)

        val nameUser2 = "User2"
        val portUser2 = 8084
        val (chatUser2, serverUser2, serverJobUser2) = addUser(nameUser2, host, portUser2, Protocol.WEBSOCKET)
        (chatUser1 as Chat).updateUsers()
        (chatUser2 as Chat).updateUsers()

        val sentMessageUser1 = mutableListOf<String>()
        val sentMessageUser2 = mutableListOf<String>()

        for (i in 1..3) {

            val messageUser1 = "abc$i"
            sentMessageUser1.add(messageUser1)
            chatUser1.testMessageSent(nameUser2, messageUser1)

            val messageUser2 = ":)$i"
            sentMessageUser2.add(messageUser2)
            chatUser2.testMessageSent(nameUser1, messageUser2)

        }

        assertEquals(sentMessageUser1, chatUser2.allMessages)
        assertEquals(sentMessageUser2, chatUser1.allMessages)

        deleteUser(nameUser1, serverUser1 as WebSocketChatServer, serverJobUser1 as Thread, Protocol.WEBSOCKET)
        deleteUser(nameUser2, serverUser2 as WebSocketChatServer, serverJobUser2 as Thread, Protocol.WEBSOCKET)

    }

}