package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TcpListener(val address: Address, isSyslogFrame: Boolean) {
    private val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val queue = MessageQueue(ConfigOption.FROM_TCP_BUFFER_MESSAGES_COUNT)

    private val emitterThread = QueueEmitterThread(isListenerDeleted, router, queue, "from-tcp-$address-emitter")

    private val receiver = when (isSyslogFrame) {
        true -> SyslogFrameTcpReceiver(queue)
        else -> NewlineTerminatedTcpReceiver(queue)
    }

    fun destroy() {
        SelectorThread.removeTcpListener(address)
        isListenerDeleted.set(true)
        listeners.remove(address)
    }

    init {
        emitterThread.start()

        SelectorThread.addTcpListener(address) {
            receiver.receiveMessage(it)
        }

        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before(app: Beholder) {
            }

            override fun after(app: Beholder) {
                if (!router.hasSubscribers()) {
                    // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    Beholder.reloadListeners.remove(this)
                    destroy()
                }
            }
        })
    }

    companion object {
        private val listeners = ConcurrentHashMap<Address, TcpListener>()
        private val listenerModes = ConcurrentHashMap<Address, Boolean>()

        fun setListenerMode(address: Address, isSyslogFrame: Boolean): Boolean {
            synchronized(listenerModes) {
                if (listenerModes.containsKey(address)) {
                    if (listenerModes[address] != isSyslogFrame) {
                        return false
                    }
                } else {
                    listenerModes[address] = isSyslogFrame
                }
            }
            return true
        }

        fun getListener(address: Address): TcpListener {
            val listener = listeners[address]
            if (listener != null) {
                return listener
            }
            synchronized(listeners) {
                val isSyslogFrame = listenerModes[address]
                if (isSyslogFrame == null) {
                    throw BeholderException("TCP listener: invalid initialization order")
                }
                val newListener = listeners[address] ?: TcpListener(address, isSyslogFrame)
                listeners[address] = newListener
                return newListener
            }
        }

        fun destroyAllListeners() {
            for (listener in listeners.values) {
                listener.destroy()
            }
        }
    }
}
