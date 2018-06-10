package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageQueue
import ru.agalkin.beholder.MessageRouter
import java.util.concurrent.atomic.AtomicBoolean

class InternalLogListener {
    private val queue = MessageQueue(ConfigOption.FROM_INTERNAL_LOG_BUFFER_MESSAGES_COUNT)

    val router = MessageRouter()

    private val emitterThread = QueueEmitterThread(AtomicBoolean(false), router, queue, "internal-log-emitter")
    init {
        emitterThread.start()
        InternalLog.listeners.add(this)
    }

    fun destroy() {
        InternalLog.listeners.remove(this)
    }

    fun add(message: Message) {
        queue.add(message)
    }
}
