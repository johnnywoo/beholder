package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.queue.BeholderQueue
import ru.agalkin.beholder.readInputStreamAndDiscard
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

const val TO_TCP_CONNECT_TIMEOUT_MILLIS = 2000

class TcpSender(app: Beholder, private val address: Address) {
    private val inetSocketAddress = InetSocketAddress(address.getInetAddress(), address.port)

    private val reconnectIntervalSeconds = AtomicInteger()

    private val queue = BeholderQueue<FieldValue>(app, ConfigOption.TO_TCP_BUFFER_MESSAGES_COUNT) { fieldValue ->
        while (true) {
            val socket = connect()
            try {
                val outputStream = socket.getOutputStream()
                outputStream.write(fieldValue.toByteArray(), 0, fieldValue.getByteLength())
                outputStream.flush()
                break
            } catch (e: Throwable) {
                InternalLog.exception(e)
            }
        }
    }

    private var socket = Socket()
    private fun connect(): Socket {
        if (socket.isConnected) {
            return socket
        }

        socket.close()
        socket = Socket()

        try {
            val sleepChunkMillis = 50L
            var toSleepMillis = reconnectIntervalSeconds.get() * 1000L
            while (toSleepMillis > 0) {
                Thread.sleep(sleepChunkMillis)
                // если вдруг reconnectIntervalSeconds изменился в процессе ожидания (например, по SIGHUP),
                // значит надо перестать ждать
                toSleepMillis = min(toSleepMillis - sleepChunkMillis, reconnectIntervalSeconds.get() * 1000L)
            }

            InternalLog.info("Attempting connection to TCP $address")
            socket.connect(inetSocketAddress, TO_TCP_CONNECT_TIMEOUT_MILLIS)

            reconnectIntervalSeconds.set(0)

            InternalLog.info("Connected to TCP $address")

            socket.keepAlive = true

            // ignore any input from the connection
            readInputStreamAndDiscard(socket.getInputStream(), "tcp-skipper")

            return socket
        } finally {
            if (!socket.isConnected) {
                // socket was not connected
                // increase the waiting interval
                val newIntervalSeconds = (reconnectIntervalSeconds.get() * 2).coerceIn(1, 60)
                reconnectIntervalSeconds.set(newIntervalSeconds)
                InternalLog.info("Will reconnect to TCP $address after $newIntervalSeconds seconds")
            }
        }
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        queue.add(fieldValue)
    }

    private var referenceCount = 0

    fun incrementReferenceCount() {
        referenceCount++
    }

    fun decrementReferenceCount() {
        referenceCount--
    }

    init {
        app.afterReloadCallbacks.add {
            // Пока система работает, она пытается переподключить тухлое соединение
            // с нарастающим интервалом.
            // После перезагрузки конфига надо сразу пробовать переподключиться заново.
            reconnectIntervalSeconds.set(0)

            // config reload is finished, our sender has no references
            // this means we should drop the connection
            if (referenceCount == 0) {
                app.tcpSenders.senders.remove(address)
                socket.close()
            }
        }
    }

    fun destroy() {
        // writerThread.isWriterDestroyed.set(true)
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
