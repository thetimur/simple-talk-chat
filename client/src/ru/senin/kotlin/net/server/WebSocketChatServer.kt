package ru.senin.kotlin.net.server

import com.google.gson.Gson
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

    override fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    private fun configureModule(): Application.() -> Unit = {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
            timeout = Duration.ofSeconds(15)
            maxFrameSize =
                Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
            masking = false
        }

        routing {
            webSocket("/v1/ws/message") { // websocketSession
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val message = Gson().fromJson(frame.readText(), Message::class.java)
                            listener?.messageReceived(message.user, message.text)
                            call.respond(mapOf("status" to "ok"))
                        }
                    }
                }
            }
        }
    }
}
