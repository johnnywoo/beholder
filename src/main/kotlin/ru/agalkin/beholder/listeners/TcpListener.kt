package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.config.Address
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
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

    abstract inner class ConnectionThreadAbstract(private val connection: Socket, name: String) : Thread(name) {
        protected fun createMessage(data: FieldValue) {
            val remoteSocketAddress = connection.remoteSocketAddress as? InetSocketAddress

            val message = Message()

            message.setFieldValue("payload", data)

            message["date"] = curDateIso()
            message["from"] = "tcp://${remoteSocketAddress?.hostString}:${remoteSocketAddress?.port}"

            queue.add(message)
        }

        // 2017-11-26T16:16:01+03:00
        // 2017-11-26T16:16:01Z if UTC
        private val formatter = getIsoDateFormatter()

        private fun curDateIso(): String
            = formatter.format(Date())

    }

    inner class SyslogFrameConnectionThread(private val connection: Socket) : ConnectionThreadAbstract(connection, "tcp-sf-connection-${connection.inetAddress.hostAddress}") {
        override fun run() {
            try {
                connection.use {
                    val inputStream = connection.getInputStream()
                    while (true) {
                        val length = readLength(inputStream)
                        if (length < 0) {
                            break
                        }
                        if (length == 0) {
                            continue
                        }

                        val data = readData(inputStream, length)

                        createMessage(FieldValue.fromByteArray(data, data.size))
                    }
                }
            } catch (e: Throwable) {
                InternalLog.exception(e)
            }
        }

        private fun readLength(input: InputStream): Int {
            var sb = StringBuilder()
            while (true) {
                val number = input.read()
                if (number < 0) {
                    return -1
                }
                val char = number.toChar()

                if (char == ' ') {
                    return sb.toString().toInt()
                }

                if (char !in '0'..'9') {
                    // ignore invalid chars, start over
                    sb = StringBuilder()
                    continue
                }

                sb.append(char)
            }
        }

        private fun readData(input: InputStream, length: Int): ByteArray {
            val buffer = ByteArray(length)
            if (input.readNBytes(buffer, 0, length) != length) {
                throw BeholderException("TCP listener: could not read expected $length bytes of data")
            }
            return buffer
        }
    }

    inner class NewlineTerminatedConnectionThread(private val connection: Socket) : ConnectionThreadAbstract(connection, "tcp-nl-connection-${connection.inetAddress.hostAddress}") {
        override fun run() {
            try {
                connection.use {
                    val inputStream = connection.getInputStream()
                    while (!connection.isClosed) {
                        val data = readInputStreamTerminated(inputStream, '\n')
                        if (data == null) {
                            // end of stream
                            break
                        }
                        if (data.isNotEmpty()) {
                            // do not include the newline in payload
                            var newlineLength = 0
                            if (data[data.size - 1].toChar() == '\n') {
                                newlineLength++
                                if (data.size >= 2 && data[data.size - 2].toChar() == '\r') {
                                    newlineLength++
                                }
                            }
                            createMessage(FieldValue.fromByteArray(data, data.size - newlineLength))
                        }
                    }
                }
            } catch (e: Throwable) {
                InternalLog.exception(e)
            }
        }

        /**
         * Reads bytes from input stream until a terminating char,
         * returns all received bytes including the terminator.
         *
         * This is not very efficient!
         */
        private fun readInputStreamTerminated(input: InputStream, stopAt: Char): ByteArray? {
            val stopAtByte = stopAt.toByte()
            val bytes = mutableListOf<Byte>()
            while (true) {
                val number = input.read()
                if (number < 0) {
                    if (bytes.isEmpty()) {
                        return null
                    }
                    return bytes.toByteArray()
                }
                val byte = number.toByte()
                bytes.add(byte)
                if (byte == stopAtByte) {
                    break
                }
            }
            return bytes.toByteArray()
        }
    }
}
