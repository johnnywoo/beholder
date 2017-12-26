package ru.agalkin.beholder.threads

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

const val FROM_UDP_MAX_BUFFER_COUNT  = 1000 // messages
const val FROM_UDP_MAX_MESSAGE_CHARS = 60 * 1024

class UdpListener(val address: Address) {
    val queue = LinkedBlockingQueue<Message>()

    val isEmitterPaused = AtomicBoolean(false)
    val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val listenerThread = UdpListenerThread(this)
    private val emitterThread  = UdpEmitterThread(this)

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before() {
                // перед тем, как заменять конфиг приложения,
                // мы хотим поставить приём сообщений на паузу
                isEmitterPaused.set(true)
            }

            override fun after() {
                if (!router.hasSubscribers()) {
                   // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    isListenerDeleted.set(true)
                    Beholder.reloadListeners.remove(this)
                    listeners.remove(address)
                }
                isEmitterPaused.set(false)
            }
        })

        listenerThread.start()
        emitterThread.start()
    }

    companion object {
        private val listeners = ConcurrentHashMap<Address, UdpListener>()

        fun getListener(address: Address): UdpListener {
            val listener = listeners[address]
            if (listener != null) {
                return listener
            }
            synchronized(listeners) {
                val newListener = listeners[address] ?: UdpListener(address)
                listeners[address] = newListener
                return newListener
            }
        }

        @Volatile private var maxReceivedPacketSize = 0

        fun getAndResetMaxReceivedPacketSize(): Int {
            val udpMaxBytesIn = maxReceivedPacketSize
            synchronized(this) {
                maxReceivedPacketSize = 0
            }
            return udpMaxBytesIn
        }

        fun updateMaxReceivedPacketSize(size: Int) {
            if (maxReceivedPacketSize < size) {
                synchronized(this) {
                    if (maxReceivedPacketSize < size) {
                        maxReceivedPacketSize = size
                    }
                }
            }
        }
    }
}
