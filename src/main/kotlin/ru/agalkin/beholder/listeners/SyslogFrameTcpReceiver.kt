package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.stats.Stats
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class SyslogFrameTcpReceiver(
    app: Beholder,
    queue: MessageQueue,
    private val address: Address
) : TcpMessageReceiverAbstract(app, queue), SelectorThread.Callback {

    override fun getAddress()
        = address

    override fun processSocketChannel(channel: SocketChannel) {
        try {
            val remoteSocketAddress = channel.remoteAddress as? InetSocketAddress

            val lengthStr = readLength(channel)
            if (lengthStr == null) {
                return
            }
            val length = lengthStr.toIntOrNull()
            if (length == null || length == 0) {
                return
            }

            val data = readData(channel, length)

            createMessage(FieldValue.fromByteArray(data, data.size), remoteSocketAddress)

            Stats.reportTcpReceived((lengthStr.length + 1 + data.size).toLong())
        } catch (e: Throwable) {
            InternalLog.exception(e)
            channel.close()
        }
    }

    private fun readLength(input: SocketChannel): String? {
        val buffer = ByteBuffer.allocate(1)
        buffer.rewind()
        var sb = StringBuilder()
        while (true) {
            val number = readCarefully(input, buffer)
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
            if (readCarefully(input, buffer) < 0) {
                input.close()
                break
            }
        }
        if (buffer.hasRemaining()) {
            throw BeholderException("TCP listener: could not read expected $length bytes of data")
        }
        buffer.rewind()
        val ba = ByteArray(length)
        buffer.get(ba)
        return ba
    }
}
