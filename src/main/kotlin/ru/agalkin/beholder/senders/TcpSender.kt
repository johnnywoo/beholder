package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.queue.FieldValueQueue
import ru.agalkin.beholder.queue.Received
import ru.agalkin.beholder.readInputStreamAndDiscard
import ru.agalkin.beholder.stats.Stats
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

const val TO_TCP_CONNECT_TIMEOUT_MILLIS = 2000

class TcpSender(app: Beholder, private val address: Address) {
    private val inetSocketAddress = InetSocketAddress(address.getInetAddress(), address.port)

    private val reconnectIntervalSeconds = AtomicInteger()

    private val queue = FieldValueQueue(app) { fieldValue ->
        try {
            val socket = connect()
            val outputStream = socket.getOutputStream()
            val byteLength = fieldValue.getByteLength()
            outputStream.write(fieldValue.toByteArray(), 0, byteLength)
            outputStream.flush()
            Stats.reportTcpSent(byteLength.toLong())
            Received.OK
        } catch (e: ConnectException) {
            socket.close()
            InternalLog.err("Cannot connect to TCP $address: ${e.message}")
            Received.RETRY
        } catch (e: SocketException) {
            socket.close()
            InternalLog.err("TCP error connected to $address: ${e.message}")
            Received.RETRY
        } catch (e: Throwable) {
            socket.close()
            InternalLog.exception(e)
            Received.RETRY
        }
    }

    fun getQueueOnlyForTests(): FieldValueQueue {
        return queue
    }

    private var socket = Socket()
    private fun connect(): Socket {
        if (socket.isConnected && !socket.isClosed) {
            return socket
        }

        socket.close()
        val connection = Socket()
        socket = connection

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
            readInputStreamAndDiscard(socket.getInputStream(), "tcp-skipper") {
                InternalLog.info("Closing client connection to TCP $address: input stream ended")
                try {
                    connection.close()
                } catch (e: Throwable) {
                    InternalLog.exception(e)
                }
            }

            return socket
        } finally {
            if (!socket.isConnected) {
                // socket was not connected
                reconnectIntervalSeconds.set(1)
                InternalLog.info("Will reconnect to TCP $address in 1 second")
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
            // с интервалом 1 сек.
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
    }
}
