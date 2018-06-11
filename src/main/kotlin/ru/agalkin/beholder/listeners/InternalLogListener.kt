package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.queue.BeholderQueue
import java.util.concurrent.atomic.AtomicBoolean

class InternalLogListener(app: Beholder) {
    val router = MessageRouter()

    private val queue = BeholderQueue<Message>(app, ConfigOption.FROM_INTERNAL_LOG_BUFFER_MESSAGES_COUNT) {
        router.sendMessageToSubscribers(it)
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
