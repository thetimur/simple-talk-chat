package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.databind.SerializationFeature
import io.github.rybalkinsd.kohttp.ext.httpGet
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.checkUserName
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.hashMapOf
import kotlin.collections.iterator
import kotlin.collections.mapOf
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.concurrent.thread


fun main(args: Array<String>) {
    thread {
        val badRequestCounter: HashMap<UserInfo, Int> = hashMapOf()
        while (true) {
            val removeList = mutableListOf<String>()
            for (user in Registry.users) {
                var response : Boolean? = null
                if (user.value.protocol == Protocol.HTTP) {
                    response = try {
                        "http://${user.value.host}:${user.value.port}/v1/health".httpGet().isSuccessful
                    } catch (e : Throwable) {
                        null
                    }
                }  else if (user.value.protocol == Protocol.WEBSOCKET) {
                    response = try {
                        Request.Builder()
                            .url("ws://$${user.value.host}:${user.value.port}")
                            .build()
                        true
                    } catch (e : Throwable) {
                        null
                    }
                }
                val now = badRequestCounter[UserInfo(user.key, user.value)] ?: 0
                if (response == null || response == false) {
                    badRequestCounter[UserInfo(user.key, user.value)] = now + 1
                    if (now > 3) {
                        removeList.add(user.key)
                        badRequestCounter.remove(UserInfo(user.key, user.value))
                    }
                } else {
                    badRequestCounter[UserInfo(user.key, user.value)] = 0
                }
                println(badRequestCounter)
            }
            for (user in removeList) {
                Registry.users.remove(user)
            }
            sleep(60 * 1000)
        }
    }
    EngineMain.main(args)
}

object Registry {
    val users = ConcurrentHashMap<String, UserAddress>()
}

@Suppress("UNUSED_PARAMETER")
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "invalid argument")
        }
        exception<UserAlreadyRegisteredException> { cause ->
            call.respond(HttpStatusCode.Conflict, cause.message ?: "user already registered")
        }
        exception<IllegalUserNameException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "illegal user name")
        }
    }
    routing {
        get("/v1/health") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }

        post("/v1/users") {
            val user = call.receive<UserInfo>()
            val name = user.name
            checkUserName(name) ?: throw IllegalUserNameException()
            if (Registry.users.contains(name)) {
                throw UserAlreadyRegisteredException()
            }
            Registry.users[name] = user.address
            call.respond(mapOf("status" to "ok"))
        }

        get("/v1/users") {
            call.respond(Registry.users)
        }

        put("/v1/users/{user}") {
            val user = call.receive<UserInfo>()

            if (!Registry.users.containsKey(user.name)) {
                throw UserNotRegisteredException()
            }
            checkUserName(user.name) ?: throw IllegalUserNameException()
            Registry.users[user.name] = user.address
            call.respond(mapOf("status" to "ok"))
        }

        delete("/v1/users/{user}") {
            val name = call.parameters["user"]

            LoggerFactory.getLogger("$name")

            if (name == null) {
                throw UserNotRegisteredException()
            }
            checkUserName(name) ?: throw IllegalUserNameException()
            Registry.users.remove(name)
            call.respond(mapOf("status" to "ok"))
        }
    }
}

class UserAlreadyRegisteredException: RuntimeException("User already registered")
class UserNotRegisteredException: RuntimeException("User not registered")
class IllegalUserNameException: RuntimeException("Illegal user name")