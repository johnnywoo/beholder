package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.MessageQueue
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.ConfigOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

const val FROM_UDP_MAX_MESSAGE_BYTES = 65507

class UdpListener(private val app: Beholder, val address: Address) {
    val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val queue = MessageQueue(ConfigOption.FROM_UDP_BUFFER_MESSAGES_COUNT)

    private val emitterThread  = QueueEmitterThread(isListenerDeleted, router, queue, "from-udp-$address-emitter")
    private val listenerThread = UdpListenerThread(this, queue)

    fun destroy() {
        isListenerDeleted.set(true)
        app.udpListeners.listeners.remove(address)
    }

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before(app: Beholder) {
            }

            override fun after(app: Beholder) {
                if (!router.hasSubscribers()) {
                    // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    destroy()
                    Beholder.reloadListeners.remove(this)
                }
            }
        })

        listenerThread.start()
        emitterThread.start()
    }

    class Factory(private val app: Beholder) {
        val listeners = ConcurrentHashMap<Address, UdpListener>()

        fun getListener(address: Address): UdpListener {
            val listener = listeners[address]
            if (listener != null) {
                return listener
            }
            synchronized(listeners) {
                val newListener = listeners[address] ?: UdpListener(app, address)
                listeners[address] = newListener
                return newListener
            }
        }

        fun destroyAllListeners(): Int {
            val n = listeners.size
            for (listener in listeners.values) {
                listener.destroy()
            }
            return n
        }
    }
}
