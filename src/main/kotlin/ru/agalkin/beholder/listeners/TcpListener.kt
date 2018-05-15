package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.config.Address
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.CharBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TcpListener(val address: Address, private val isSyslogFrame: Boolean) {
    val isListenerDeleted = AtomicBoolean(false)

    val router = MessageRouter()

    private val queue = MessageQueue(ConfigOption.FROM_TCP_BUFFER_MESSAGES_COUNT)

    private val emitterThread = QueueEmitterThread(isListenerDeleted, router, queue, "from-tcp-$address-emitter")

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before(app: Beholder) {
            }

            override fun after(app: Beholder) {
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
                    if (isSyslogFrame) {
                        SyslogFrameConnectionThread(connection).start()
                    } else {
                        NewlineTerminatedConnectionThread(connection).start()
                    }
                } catch (ignored: InterruptedIOException) {
                    // ждём кусками по 50 мс, чтобы проверять isListenerDeleted
                }
            }
        }
    }

    abstract inner class ConnectionThreadAbstract(private val connection: Socket) : Thread() {
        protected fun createMessage(data: String) {
            val remoteSocketAddress = connection.remoteSocketAddress as? InetSocketAddress

            val message = Message()

            message["payload"] = data
            message["date"]    = curDateIso()
            message["from"]    = "tcp://${remoteSocketAddress?.address}:${remoteSocketAddress?.port}"

            queue.add(message)
        }

        // 2017-11-26T16:16:01+03:00
        // 2017-11-26T16:16:01Z if UTC
        private val formatter = getIsoDateFormatter()

        private fun curDateIso(): String
            = formatter.format(Date())

    }

    inner class SyslogFrameConnectionThread(private val connection: Socket) : ConnectionThreadAbstract(connection) {
        override fun run() {
            try {
                connection.getInputStream().use { inputStream ->
                    val input = InputStreamReader(inputStream)
                    while (true) {
                        val length = readLength(input)
                        val data = readData(input, length)

                        createMessage(data)
                    }
                }
            } catch (e: Throwable) {
                InternalLog.exception(e)
            }
        }

        private fun readLength(input: InputStreamReader): Int {
            val sb = StringBuilder()
            val buffer = CharBuffer.allocate(1)
            buffer.rewind()
            while (input.read(buffer) > 0) {
                val char = buffer[0]
                buffer.rewind()

                if (char == ' ') {
                    return sb.toString().toInt()
                }

                if (char in '0'..'9') {
                    sb.append(char)
                } else {
                    throw BeholderException("TCP syslog-frame: invalid char in length prefix '$char'")
                }
            }
            throw BeholderException("TCP syslog-frame: could not read length of frame, received '$sb'")
        }

        private fun readData(input: InputStreamReader, length: Int): String {
            val buffer = CharBuffer.allocate(length)
            buffer.rewind()
            if (input.read(buffer) != length) {
                throw BeholderException("TCP listener: could not read expected $length bytes of data")
            }
            return buffer.toString()
        }
    }

    inner class NewlineTerminatedConnectionThread(private val connection: Socket) : ConnectionThreadAbstract(connection) {
        override fun run() {
            try {
                connection.getInputStream().use { inputStream ->
                    val input = BufferedReader(InputStreamReader(inputStream))
                    while (true) {
                        val line = input.readLine()
                        if (line == null) {
                            break
                        }

                        createMessage(line)
                    }
                }
            } catch (e: Throwable) {
                InternalLog.exception(e)
            }
        }
    }
}
