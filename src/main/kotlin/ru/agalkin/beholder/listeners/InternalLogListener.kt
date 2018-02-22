package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter
import java.util.concurrent.atomic.AtomicBoolean

class InternalLogListener {
    val emitterThread = QueueEmitterThread(AtomicBoolean(false), router, "internal-log-emitter")
    init {
        emitterThread.start()
    }

    companion object {
        private val instance by lazy { InternalLogListener() }

        private var ignoreAllMessages = true

        private val router = MessageRouter()

        fun getMessageRouter(): MessageRouter {
            ignoreAllMessages = false
            return router
        }

        fun add(message: Message) {
            // не создаём instance и с ним тред, пока кто-нибудь не подпишется на наш лог
            if (!ignoreAllMessages) {
                instance.emitterThread.queue.offer(message)
            }
        }
    }
}
