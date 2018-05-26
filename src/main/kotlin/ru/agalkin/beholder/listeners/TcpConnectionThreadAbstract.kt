package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageQueue
import ru.agalkin.beholder.getIsoDateFormatter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

abstract class TcpConnectionThreadAbstract(
    private val connection: Socket,
    private val queue: MessageQueue,
    name: String
) : Thread(name) {

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
