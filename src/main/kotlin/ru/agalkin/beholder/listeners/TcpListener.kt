package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.queue.Received
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TcpListener(val app: Beholder, val address: Address, isSyslogFrame: Boolean) {
    private val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val queue = MessageQueue(app) {
        router.sendMessageToSubscribers(it)
        Received.OK
    }

    fun getQueueOnlyForTests(): MessageQueue {
        return queue
    }

    private val receiver: SelectorThread.Callback = when (isSyslogFrame) {
        true -> SyslogFrameTcpReceiver(app, queue, address)
        else -> NewlineTerminatedTcpReceiver(app, queue, address)
    }

    fun destroy() {
        app.selectorThread.removeTcpListener(address)
        isListenerDeleted.set(true)
        app.tcpListeners.listeners.remove(address)
    }

    init {
        app.selectorThread.addTcpListener(receiver)

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
