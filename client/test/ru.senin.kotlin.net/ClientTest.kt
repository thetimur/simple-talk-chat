package ru.senin.kotlin.net

import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.server.HttpChatServer
import ru.senin.kotlin.net.server.UdpChatServer
import ru.senin.kotlin.net.server.WebSocketChatServer
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ClientTest {

    fun testInsistance(protocol: Protocol, optionalPort: Int) {
        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = optionalPort
        val chatUser1 = addUser(nameUser1, host, portUser1, protocol)

        val nameUser2 = "User2"
        val portUser2 = optionalPort + 1
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
            sleep(100)

            val messageUser2 = ":)$i"
            sentMessageUser2.add(messageUser2)
            chatUser2.testMessageSent(nameUser1, messageUser2, protocol, host, portUser1)
            sleep(100)
        }

        assertEquals(sentMessageUser1, chatUser2.getAllMessages())
        assertEquals(sentMessageUser2, chatUser1.getAllMessages())
    }

    private fun addUser (name: String, host: String, port: Int, protocol: Protocol): Chat {
        val server = when(protocol) {
            Protocol.HTTP -> HttpChatServer(host, port)
            Protocol.WEBSOCKET -> WebSocketChatServer(host, port)
            Protocol.UDP -> UdpChatServer(host, port)
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
        testInsistance(Protocol.HTTP, 8081)
    }

    @Test
    fun testClientWebSocket () {
        testInsistance(Protocol.WEBSOCKET, 9091)
    }

    @Test
    fun testClientUDP () {
        testInsistance(Protocol.UDP, 3031)
    }

}