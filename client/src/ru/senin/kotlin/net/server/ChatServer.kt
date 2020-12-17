package ru.senin.kotlin.net.server

interface ChatServer {
    fun setMessageListener(listener: ChatMessageListener)
    fun start()
    fun stop()
}