package ru.senin.kotlin.net

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.server.HttpChatServer
import ru.senin.kotlin.net.server.UdpChatServer
import ru.senin.kotlin.net.server.WebSocketChatServer
import java.lang.System.exit
import java.net.URL
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class Parameters : Arkenv() {
    val name : String by argument("--name") {
        description = "Name of user"
    }

    val registryBaseUrl : String by argument("--registry"){
        description = "Base URL of User Registry"
        defaultValue = { "http://localhost:8088" }
    }

    val host : String by argument("--host"){
        description = "Hostname or IP to listen on"
        defaultValue = { "0.0.0.0" } // 0.0.0.0 - listen on all network interfaces
    }

    val port : Int by argument("--port") {
        description = "Port to listen for on"
        defaultValue = { 8080 }
    }

    val publicUrl : String? by argument("--public-url") {
        description = "Public URL"
    }

    val protocol: Protocol by argument("--protocol") {
        defaultValue = { Protocol.HTTP }
        mapping = {
            when(it) {
                "websocket" -> Protocol.WEBSOCKET
                "udp" -> Protocol.UDP
                else -> Protocol.HTTP
            }
        }
    }
}

val log: Logger = LoggerFactory.getLogger("main")
lateinit var parameters : Parameters

fun main(args: Array<String>) {
    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }
        val host = parameters.host
        val port = parameters.port
      
        val protocol = parameters.protocol

        // validate host and port
        if (port !in 0..65536) {
            println ("bad port $port")
            exitProcess(1)
        }
        if (!host.matches(Regex( "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)" +
            "*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])\$"))) {
            println ("bad host $host")
            exitProcess(1)
        }

        val name = parameters.name
        checkUserName(name) ?: throw IllegalArgumentException("Illegal user name '$name'")

        // initialize registry interface
        val objectMapper = jacksonObjectMapper()
        val registry = Retrofit.Builder()
            .baseUrl(parameters.registryBaseUrl)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build().create(RegistryApi::class.java)

        // create server engine
        val server = when(protocol) {
            Protocol.HTTP -> HttpChatServer(host, port)
            Protocol.WEBSOCKET -> WebSocketChatServer(host, port)
            Protocol.UDP -> UdpChatServer(host, port)
        }
        val chat = Chat(name, registry)
        server.setMessageListener(chat)

        // start server as separate job
        val serverJob = thread {
            server.start()
        }
        try {
            // register our client
            val userAddress  = when {
                parameters.publicUrl != null -> {
                    val url = URL(parameters.publicUrl)
                    when (url.port) {
                        in 1..65536 -> UserAddress(protocol, url.host, url.port)
                        else -> UserAddress(protocol, url.host, 80)
                    }
                }
                else -> UserAddress(protocol, host, port)
            }
            registry.register(UserInfo(name, userAddress)).execute()

            // start
            chat.commandLoop()
        }
        finally {
            registry.unregister(name).execute()
            server.stop()
            serverJob.join()
        }
    }
    catch (e: Exception) {
        log.error("Error! ${e.message}", e)
        println("Error! ${e.message}")
    }
}
