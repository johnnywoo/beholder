package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.queue.MessageQueue
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

abstract class TcpMessageReceiverAbstract(
    private val app: Beholder,
    private val queue: MessageQueue
) {
    abstract fun receiveMessage(socketChannel: SocketChannel)

    protected fun createMessage(data: FieldValue, channel: SocketChannel) {
        val remoteSocketAddress = channel.remoteAddress as? InetSocketAddress

        val message = Message()

        message.setFieldValue("payload", data)

        message["date"] = app.curDateIso()
        message["from"] = "tcp://${remoteSocketAddress?.hostString}:${remoteSocketAddress?.port}"

        queue.add(message)
    }
}
