package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.queue.Received

class InternalLogListener(app: Beholder) {
    val router = MessageRouter()

    private val queue = MessageQueue(app) {
        router.sendMessageToSubscribers(it)
        Received.OK
    }

    init {
        InternalLog.listeners.add(this)
    }

    fun destroy() {
        InternalLog.listeners.remove(this)
    }

    fun add(message: Message) {
        queue.add(message)
    }
}
