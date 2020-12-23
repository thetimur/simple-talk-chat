package ru.senin.kotlin.net

import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.server.HttpChatServer
import ru.senin.kotlin.net.server.UdpChatServer
import ru.senin.kotlin.net.server.WebSocketChatServer
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ClientTest {


    private fun addUser (name: String, host: String, port: Int, protocol: Protocol): Chat {

        val server = when(protocol) {
            Protocol.HTTP -> HttpChatServer(host, port)
            Protocol.WEBSOCKET -> WebSocketChatServer(host, port)
            Protocol.UDP -> UdpChatServer(host, port)
            else -> UdpChatServer(host, port)
        }

        val chat = Chat(name, null)
        server.setMessageListener(chat)

        thread {
            server.start()
        }

        return chat
    }

    @Test
    fun testClientHTTP () {

        val protocol = Protocol.HTTP
        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = 8081
        val chatUser1 = addUser(nameUser1, host, portUser1, protocol)

        val nameUser2 = "User2"
        val portUser2 = 8082
        val chatUser2 = addUser(nameUser2, host, portUser2, protocol)
        chatUser1.updateUsers()
        chatUser2.updateUsers()

        val sentMessageUser1 = mutableListOf<String>()
        val sentMessageUser2 = mutableListOf<String>()

        sleep(1000)

        for (i in 1..3) {

            val messageUser1 = "abc$i"
            sentMessageUser1.add(messageUser1)
            chatUser1.testMessageSent(nameUser2, messageUser1, protocol, host, portUser2)

            val messageUser2 = ":)$i"
            sentMessageUser2.add(messageUser2)
            chatUser2.testMessageSent(nameUser1, messageUser2, protocol, host, portUser1)

        }

        assertEquals(sentMessageUser1, chatUser2.getAllMessages())
        assertEquals(sentMessageUser2, chatUser1.getAllMessages())
    }

    @Test
    fun testClientWebSocket () {

        val protocol = Protocol.WEBSOCKET
        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = 8090
        val chatUser1 = addUser(nameUser1, host, portUser1, protocol)

        val nameUser2 = "User2"
        val portUser2 = 8091
        val chatUser2 = addUser(nameUser2, host, portUser2, protocol)
        chatUser1.updateUsers()
        chatUser2.updateUsers()

        sleep(1000)

        val sentMessageUser1 = mutableListOf<String>()
        val sentMessageUser2 = mutableListOf<String>()

        for (i in 1..3) {

            val messageUser1 = "abc$i"
            sentMessageUser1.add(messageUser1)
            chatUser1.testMessageSent(nameUser2, messageUser1, protocol, host, portUser2)

            val messageUser2 = ":)$i"
            sentMessageUser2.add(messageUser2)
            chatUser2.testMessageSent(nameUser1, messageUser2, protocol, host, portUser1)

            sleep(1000)

        }

        assertEquals(sentMessageUser1, chatUser2.getAllMessages())
        assertEquals(sentMessageUser2, chatUser1.getAllMessages())
    }

    @Test
    fun testClientUDP () {

        val protocol = Protocol.UDP
        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = 3000
        val chatUser1 = addUser(nameUser1, host, portUser1, protocol)

        val nameUser2 = "User2"
        val portUser2 = 3001
        val chatUser2 = addUser(nameUser2, host, portUser2, protocol)
        chatUser1.updateUsers()
        chatUser2.updateUsers()

        val sentMessageUser1 = mutableListOf<String>()
        val sentMessageUser2 = mutableListOf<String>()

        sleep(2000)

        for (i in 1..3) {

            val messageUser1 = "abc$i"
            sentMessageUser1.add(messageUser1)
            chatUser1.testMessageSent(nameUser2, messageUser1, protocol, host, portUser2)

            val messageUser2 = ":)$i"
            sentMessageUser2.add(messageUser2)
            chatUser2.testMessageSent(nameUser1, messageUser2, protocol, host, portUser1)
            sleep(1000)
        }

        assertEquals(sentMessageUser1, chatUser2.getAllMessages())
        assertEquals(sentMessageUser2, chatUser1.getAllMessages())
    }

}