package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.MessageQueue
import ru.agalkin.beholder.stats.Stats
import java.io.InputStream
import java.net.Socket

class SyslogFrameTcpConnectionThread(
    private val connection: Socket,
    queue: MessageQueue
) : TcpConnectionThreadAbstract(
    connection,
    queue,
    "tcp-sf-connection-${connection.inetAddress.hostAddress}"
) {
    override fun run() {
        try {
            connection.use {
                val inputStream = connection.getInputStream()
                while (true) {
                    val lengthStr = readLength(inputStream)
                    if (lengthStr == null) {
                        break
                    }
                    val length = lengthStr.toInt()
                    if (length == 0) {
                        continue
                    }

                    val data = readData(inputStream, length)

                    createMessage(FieldValue.fromByteArray(data, data.size))

                    Stats.reportTcpReceived((lengthStr.length + 1 + data.size).toLong())
                }
            }
        } catch (e: Throwable) {
            InternalLog.exception(e)
        }
    }

    private fun readLength(input: InputStream): String? {
        var sb = StringBuilder()
        while (true) {
            val number = input.read()
            if (number < 0) {
                return null
            }
            val char = number.toChar()

            if (char == ' ') {
                return sb.toString()
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
