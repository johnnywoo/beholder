package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.queue.Received
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class InfinityListener(private val app: Beholder, messageLengthBytes: Int) {
    val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val queue = MessageQueue(app) {
        router.sendMessageToSubscribers(it)
        Received.OK
    }

    private val listenerThread = InfinityListenerThread(app, this, messageLengthBytes, queue)

    fun destroy() {
        isListenerDeleted.set(true)
        app.infinityListeners.listeners.remove(this)
    }

    init {
        app.afterReloadCallbacks.add(object : () -> Unit {
            override fun invoke() {
                if (!router.hasSubscribers()) {
                    // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    app.afterReloadCallbacks.remove(this)
                    destroy()
                }
            }
        })

        listenerThread.start()
    }

    class Factory(private val app: Beholder) {
        val listeners = mutableSetOf<InfinityListener>()

        fun addListener(messageLengthBytes: Int): InfinityListener {
            synchronized(listeners) {
                val newListener = InfinityListener(app, messageLengthBytes)
                listeners.add(newListener)
                return newListener
            }
        }

        fun destroyAllListeners(): Int {
            val n = listeners.size
            for (listener in listeners) {
                listener.destroy()
            }
            return n
        }
    }
}
