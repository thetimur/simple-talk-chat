package ru.senin.kotlin.net.server

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import ru.senin.kotlin.net.Message
import java.lang.IllegalStateException
import java.net.InetSocketAddress

class UdpChatServer(private val host: String, private val port: Int) : ChatServer {

    private var listener: ChatMessageListener? = null

    private val engine = createEngine()

    private fun createEngine(): NettyApplicationEngine {
        val applicationEnvironment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("http-server")
            classLoader = ApplicationEngineEnvironment::class.java.classLoader
            connector {
                this.host = this@UdpChatServer.host
                this.port = this@UdpChatServer.port
            }
            module (configureModule())
        }
        return NettyApplicationEngine(applicationEnvironment)
    }

    override fun start() {
        engine.start(true)
    }

    override fun stop() {
        engine.stop(1000, 2000)
    }

    override fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    private fun configureModule(): Application.() -> Unit = {

        routing {
            runBlocking {
                val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(host, port))
                println("Started at ${server.localAddress}")

                while (true) {
                    val socket = server.accept()

                    launch {

                        val input = socket.openReadChannel().readUTF8Line()

                        try {
                            try {
                                val message = Gson().fromJson(input, Message::class.java)
                                listener?.messageReceived(message.user, message.text)
                            } catch (e : Exception) {
                                println("Cannot translate $input")
                                throw IllegalStateException()
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            socket.close()
                        }
                    }
                }
            }
        }
    }
}
