package ru.senin.kotlin.net.server

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.mapNotNull
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.UserInfo
import java.time.Duration

class WebSocketChatServer(private val host: String, private val port: Int) : ChatServer {
    private val objectMapper = jacksonObjectMapper()
    private var listener: ChatMessageListener? = null

    private val engine = createEngine()

    private fun createEngine(): NettyApplicationEngine {
        val applicationEnvironment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("http-server")
            classLoader = ApplicationEngineEnvironment::class.java.classLoader
            connector {
                this.host = this@WebSocketChatServer.host
                this.port = this@WebSocketChatServer.port
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

    fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    private fun configureModule(): Application.() -> Unit = {
        install(CallLogging) {
            level = Level.DEBUG
            filter { call -> call.request.path().startsWith("/") }
        }

        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
            masking = false
        }

        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }

        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }

        routing {
            webSocket("/v1/ws/message") {
                for (frame in incoming.mapNotNull { it as? Frame.Text }) {
                    val text = frame.readText()
                    val message = Gson().fromJson(text, Message::class.java)
                    if (listener != null) {
                        listener?.messageReceived(message.user, message.text)
                        call.respond(mapOf("OK" to true))
                    } else {
                        call.respond(mapOf("OK" to false))
                    }
                }
            }
        }
    }
}

// Send test message using curl:
// curl -v -X POST http://localhost:8080/v1/message -H "Content-type: application/json" -d '{ "user":"ivanov", "text":"Hello!"}'
