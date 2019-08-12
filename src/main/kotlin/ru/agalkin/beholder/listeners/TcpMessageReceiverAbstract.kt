package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.queue.MessageQueue
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

abstract class TcpMessageReceiverAbstract(
    private val app: Beholder,
    private val queue: MessageQueue
) {
    protected fun createMessage(data: FieldValue, remoteSocketAddress: InetSocketAddress?) {
        val message = Message()

        message.setFieldValue("payload", data)

        message["date"] = app.curDateIso()
        message["from"] = "tcp://${remoteSocketAddress?.hostString}:${remoteSocketAddress?.port}"

        queue.add(message)
    }

    protected fun readCarefully(input: SocketChannel, buffer: ByteBuffer): Int {
        val number = input.read(buffer)
        if (number < 0) {
            input.close()
        }
        return number
    }
}
