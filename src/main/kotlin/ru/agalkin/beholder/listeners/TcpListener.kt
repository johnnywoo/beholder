package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.getIsoDateFormatter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

const val FROM_TCP_MAX_BUFFER_COUNT  = 1000 // messages

class TcpListener(val address: Address) {
    val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val emitterThread = QueueEmitterThread(isListenerDeleted, router, "from-tcp-$address-emitter")

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before() {
            }

            override fun after() {
                if (!router.hasSubscribers()) {
                   // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    isListenerDeleted.set(true)
                    Beholder.reloadListeners.remove(this)
                    listeners.remove(address)
                }
            }
        })

        emitterThread.start()
        ListenerThread().start()
    }

    companion object {
        private val listeners = ConcurrentHashMap<Address, TcpListener>()

        fun getListener(address: Address): TcpListener {
            val listener = listeners[address]
            if (listener != null) {
                return listener
            }
            synchronized(listeners) {
                val newListener = listeners[address] ?: TcpListener(address)
                listeners[address] = newListener
                return newListener
            }
        }
    }

    inner class ListenerThread : Thread("from-tcp-$address-listener") {
        private val socket = ServerSocket(address.port, 2000, address.getInetAddress())
        init {
            socket.soTimeout = 50 // millis
        }

        override fun run() {
            while (!isListenerDeleted.get()) {
                try {
                    val connection = socket.accept()
                    ConnectionThread(connection).start()
                } catch (ignored: InterruptedIOException) {
                    // ждём кусками по 50 мс, чтобы проверять isListenerDeleted
                }
            }
        }
    }

    inner class ConnectionThread(private val connection: Socket) : Thread() {
        override fun run() {
            connection.getInputStream().use { inputStream ->
                val input = BufferedReader(InputStreamReader(inputStream))
                while (true) {
                    val line = input.readLine()
                    if (line == null) {
                        break
                    }

                    // не даём очереди бесконтрольно расти (вытесняем старые записи)
                    if (emitterThread.queue.size > FROM_TCP_MAX_BUFFER_COUNT) {
                        emitterThread.queue.take() // FIFO
                    }

                    val remoteSocketAddress = connection.remoteSocketAddress as? InetSocketAddress

                    val message = Message()

                    message["payload"]      = line
                    message["receivedDate"] = curDateIso()
                    message["from"]         = "tcp://${remoteSocketAddress?.address}:${remoteSocketAddress?.port}"

                    emitterThread.queue.offer(message)
                }
            }
        }

        // 2017-11-26T16:16:01+03:00
        // 2017-11-26T16:16:01Z if UTC
        private val formatter = getIsoDateFormatter()

        private fun curDateIso(): String
            = formatter.format(Date())
    }
}
