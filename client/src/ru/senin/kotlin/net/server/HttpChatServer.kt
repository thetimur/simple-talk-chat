package ru.senin.kotlin.net.server

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.UserInfo


interface ChatMessageListener {
    fun messageReceived(userName: String, text: String)
}

class HttpChatServer(private val host: String, private val port: Int) : ChatServer {

    private var listener: ChatMessageListener? = null

    private val engine = createEngine()

    private fun createEngine(): NettyApplicationEngine {
        val applicationEnvironment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("http-server")
            classLoader = ApplicationEngineEnvironment::class.java.classLoader
            connector {
                this.host = this@HttpChatServer.host
                this.port = this@HttpChatServer.port
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
        install(CallLogging) {
            level = Level.DEBUG
            filter { call -> call.request.path().startsWith("/") }
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
            get("v1/health") {
                call.respond(mapOf("OK" to true))
            }

            post("/v1/message") {
                val message = call.receive<Message>()
                if (listener != null) {
                    listener?.messageReceived(message.user, message.text)
                    call.respond(mapOf("OK" to true))
                } else {
                    call.respond(mapOf("OK" to false))
                }
            }

            install(StatusPages) {
                exception<IllegalArgumentException> {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}
