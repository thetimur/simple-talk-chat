package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.websocket.*
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
import io.ktor.websocket.WebSockets
import org.slf4j.LoggerFactory
import ru.senin.kotlin.net.Message
import java.time.Duration

class WebSocketChatServer(private val host: String, private val port: Int) : ChatServer {
    private var listener: ChatMessageListener? = null
    private val objectMapper = jacksonObjectMapper()
    private var isServerStarted = false
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
        isServerStarted = true
        engine.start(true)
    }

    override fun stop() {
        isServerStarted = false
        engine.stop(1000, 2000)
    }

    override fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    private fun configureModule(): Application.() -> Unit = {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
            timeout = Duration.ofSeconds(15)
            maxFrameSize =
                Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
            masking = true
        }

        routing {
            webSocket("/v1/ws/message") { // websocketSession
                while (isServerStarted) {
                    val frame = incoming.receive() as Frame.Text
                    val message = objectMapper.readValue(frame.readText(), Message::class.java)
                    listener?.messageReceived(message.user, message.text)
                }
            }
        }
    }
}
