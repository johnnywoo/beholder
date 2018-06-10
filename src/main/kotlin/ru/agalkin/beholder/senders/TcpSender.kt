package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap

const val TO_TCP_CONNECT_TIMEOUT_MILLIS = 2000

class TcpSender(app: Beholder, address: Address) {
    private val writerThread = TcpWriterThread(app, address)

    fun writeMessagePayload(fieldValue: FieldValue) {
        writerThread.queue.add(fieldValue)
    }

    private var referenceCount = 0

    fun incrementReferenceCount() {
        referenceCount++
    }

    fun decrementReferenceCount() {
        referenceCount--
    }

    init {
        app.beforeReloadCallbacks.add {
            writerThread.isWriterPaused.set(true)
        }

        app.afterReloadCallbacks.add {
            writerThread.isWriterPaused.set(false)

            // Пока система работает, она пытается переподключить тухлое соединение
            // с нарастающим интервалом.
            // После перезагрузки конфига надо сразу пробовать переподключиться заново.
            writerThread.reconnectIntervalSeconds.set(0)

            // config reload is finished, our sender has no references
            // this means we should drop the connection
            if (referenceCount == 0) {
                writerThread.isWriterDestroyed.set(true)
                app.tcpSenders.senders.remove(address)
            }
        }

        writerThread.start()
    }

    fun destroy() {
        writerThread.isWriterDestroyed.set(true)
    }

    class Factory(private val app: Beholder) {
        val senders = ConcurrentHashMap<Address, TcpSender>()

        fun getSender(address: Address): TcpSender {
            val sender = senders[address]
            if (sender != null) {
                return sender
            }
            synchronized(senders) {
                val newSender = senders[address] ?: TcpSender(app, address)
                senders[address] = newSender
                return newSender
            }
        }

        fun destroyAllSenders(): Int {
            val n = senders.size
            for (sender in senders.values) {
                sender.destroy()
            }
            return n
        }
    }
}
