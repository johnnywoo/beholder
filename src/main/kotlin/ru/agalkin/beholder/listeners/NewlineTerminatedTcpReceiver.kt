package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.queue.BeholderQueue
import ru.agalkin.beholder.stats.Stats
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class NewlineTerminatedTcpReceiver(queue: BeholderQueue<Message>) : TcpMessageReceiverAbstract(queue) {
    override fun receiveMessage(socketChannel: SocketChannel) {
        val data = readTerminated(socketChannel, '\n')
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

            createMessage(FieldValue.fromByteArray(data, data.size - newlineLength), socketChannel)

            Stats.reportTcpReceived(data.size.toLong())
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
            val number = input.read(buffer)
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
