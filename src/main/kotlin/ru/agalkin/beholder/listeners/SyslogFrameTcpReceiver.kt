package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.queue.BeholderQueue
import ru.agalkin.beholder.stats.Stats
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class SyslogFrameTcpReceiver(queue: BeholderQueue<Message>) : TcpMessageReceiverAbstract(queue) {
    override fun receiveMessage(socketChannel: SocketChannel) {
        val lengthStr = readLength(socketChannel)
        if (lengthStr == null) {
            return
        }
        val length = lengthStr.toInt()
        if (length == 0) {
            return
        }

        val data = readData(socketChannel, length)

        createMessage(FieldValue.fromByteArray(data, data.size), socketChannel)

        Stats.reportTcpReceived((lengthStr.length + 1 + data.size).toLong())
    }

    private fun readLength(input: SocketChannel): String? {
        val buffer = ByteBuffer.allocate(1)
        buffer.rewind()
        var sb = StringBuilder()
        while (true) {
            val number = input.read(buffer)
            if (number < 1) {
                return null
            }
            val char = buffer[0].toChar()
            buffer.rewind()

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

    private fun readData(input: SocketChannel, length: Int): ByteArray {
        val buffer = ByteBuffer.allocate(length)
        buffer.rewind()
        while (buffer.hasRemaining()) {
            if (input.read(buffer) < 0) {
                break
            }
        }
        if (buffer.hasRemaining()) {
            throw BeholderException("TCP listener: could not read expected $length bytes of data")
        }
        val ba = ByteArray(length)
        buffer.get(ba)
        return ba
    }
}
