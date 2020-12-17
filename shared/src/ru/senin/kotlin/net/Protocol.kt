package ru.senin.kotlin.net

enum class Protocol {
    HTTP,
    UDP,
    WEBSOCKET
}

fun Protocol.getPrefix() : String {
    return when(this) {
        Protocol.HTTP -> "http"
        Protocol.WEBSOCKET -> "ws"
        Protocol.UDP -> "udp"
    }
}

data class UserAddress(
        val protocol: Protocol,
        val host: String,
        val port: Int = 8080
) {
    override fun toString(): String {
        return "${protocol.getPrefix()}://${host}:${port}"
    }
}

data class UserInfo(val name: String, val address: UserAddress)

data class Message(val user: String, val text: String)

fun checkUserName(name: String) = """^[a-zA-Z0-9-_.]+$""".toRegex().find(name)
