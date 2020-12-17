package ru.senin.kotlin.net

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.server.HttpChatServer
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ClientTest {

    private val objectMapper = jacksonObjectMapper()
    private val registry: RegistryApi = Retrofit.Builder()
        .baseUrl("http://localhost:8088")
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .build().create(RegistryApi::class.java)

    private fun addUser (name: String, host: String, port: Int): List<Any> {

        val server = HttpChatServer(host, port)
        val chat = Chat(name, registry)
        server.setMessageListener(chat)

        val serverJob = thread {
            server.start()
        }

        registry.register(UserInfo(name, UserAddress(Protocol.HTTP, host, port))).execute()

        return listOf (chat, server, serverJob)
    }

    private fun deleteUser (name: String, server: HttpChatServer, serverJob: Thread) {
        registry.unregister(name).execute()
        server.stop()
        serverJob.join()
    }

    @Test
    fun testClientHTTP () {

        val host = "127.0.0.1"

        val nameUser1 = "User1"
        val portUser1 = 8081
        val (chatUser1, serverUser1, serverJobUser1) = addUser(nameUser1, host, portUser1)

        val nameUser2 = "User2"
        val portUser2 = 8082
        val (chatUser2, serverUser2, serverJobUser2) = addUser(nameUser2, host, portUser2)
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

        assertEquals(sentMessageUser1, chatUser2.getAllMessages())
        assertEquals(sentMessageUser2, chatUser1.getAllMessages())

        deleteUser(nameUser1, serverUser1 as HttpChatServer, serverJobUser1 as Thread)
        deleteUser(nameUser2, serverUser2 as HttpChatServer, serverJobUser2 as Thread)

    }

}