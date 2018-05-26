package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.MessageQueue
import ru.agalkin.beholder.stats.Stats
import java.io.InputStream
import java.net.Socket

class NewlineTerminatedTcpConnectionThread(
    private val connection: Socket,
    queue: MessageQueue
) : TcpConnectionThreadAbstract(
    connection,
    queue,
    "tcp-nl-connection-${connection.inetAddress.hostAddress}"
) {
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

                        Stats.reportTcpReceived(data.size.toLong())
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
