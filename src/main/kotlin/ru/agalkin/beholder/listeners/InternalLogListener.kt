package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter

class InternalLogListener {
    val emitterThread = InternalLogEmitterThread()

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before() {
                // перед тем, как заменять конфиг приложения,
                // мы хотим поставить раздачу сообщений на паузу
                emitterThread.isEmitterPaused.set(true)
            }

            override fun after() {
                emitterThread.isEmitterPaused.set(false)
            }
        })

        emitterThread.start()
    }

    companion object {
        private val instance by lazy { InternalLogListener() }

        private var ignoreAllMessages = true

        fun getMessageRouter(): MessageRouter {
            ignoreAllMessages = false
            return InternalLogEmitterThread.router
        }

        fun add(message: Message) {
            // не создаём instance и с ним тред, пока кто-нибудь не подпишется на наш лог
            if (!ignoreAllMessages) {
                instance.emitterThread.queue.offer(message)
            }
        }
    }
}
