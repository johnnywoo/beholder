package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.stats.Stats
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class NewlineTerminatedTcpReceiver(
    app: Beholder,
    queue: MessageQueue,
    private val address: Address
) : TcpMessageReceiverAbstract(app, queue), SelectorThread.Callback {

    override fun getAddress()
        = address

    override fun processSocketChannel(channel: SocketChannel) {
        synchronized(channel) {
            try {
                val remoteSocketAddress = channel.remoteAddress as? InetSocketAddress

                val data = readTerminated(channel, '\n')
                if (data == null) {
                    return
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

                    createMessage(FieldValue.fromByteArray(data, data.size - newlineLength), remoteSocketAddress)

                    Stats.reportTcpReceived(data.size.toLong())
                }
            } catch (e: Throwable) {
                InternalLog.exception(e)
                channel.close()
            }
        }
    }

    /**
     * Reads bytes from input stream until a terminating char,
     * returns all received bytes including the terminator.
     *
     * This is not very efficient!
     */
    private fun readTerminated(input: SocketChannel, stopAt: Char): ByteArray? {
        val stopAtByte = stopAt.toByte()
        val bytes = mutableListOf<Byte>()
        val buffer = ByteBuffer.allocate(1)
        while (true) {
            buffer.rewind()
            val number = readCarefully(input, buffer)
            if (number < 0) {
                if (bytes.isEmpty()) {
                    return null
                }
                return bytes.toByteArray()
            }
            val byte = buffer[0]
            bytes.add(byte)
            if (byte == stopAtByte) {
                break
            }
        }
        return bytes.toByteArray()
    }
}
