package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.queue.BeholderQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TcpListener(val app: Beholder, val address: Address, isSyslogFrame: Boolean) {
    private val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val queue = BeholderQueue<Message>(app, ConfigOption.FROM_TCP_BUFFER_MESSAGES_COUNT)

    private val emitterThread = QueueEmitterThread(app, isListenerDeleted, router, queue, "from-tcp-$address-emitter")

    private val receiver = when (isSyslogFrame) {
        true -> SyslogFrameTcpReceiver(queue)
        else -> NewlineTerminatedTcpReceiver(queue)
    }

    fun destroy() {
        app.selectorThread.removeTcpListener(address)
        isListenerDeleted.set(true)
        app.tcpListeners.listeners.remove(address)
    }

    init {
        emitterThread.start()

        app.selectorThread.addTcpListener(address) {
            receiver.receiveMessage(it)
        }

        app.afterReloadCallbacks.add(object : () -> Unit {
            override fun invoke() {
                if (!router.hasSubscribers()) {
                    // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    app.afterReloadCallbacks.remove(this)
                    destroy()
                }
            }
        })
    }

    class Factory(private val app: Beholder) {
        val listeners = ConcurrentHashMap<Address, TcpListener>()
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
                val newListener = listeners[address] ?: TcpListener(app, address, isSyslogFrame)
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
