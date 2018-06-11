package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.getIsoDateFormatter
import ru.agalkin.beholder.queue.BeholderQueue
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.util.*

abstract class TcpMessageReceiverAbstract(
    private val queue: BeholderQueue<Message>
) {
    abstract fun receiveMessage(socketChannel: SocketChannel)

    protected fun createMessage(data: FieldValue, channel: SocketChannel) {
        val remoteSocketAddress = channel.remoteAddress as? InetSocketAddress

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
