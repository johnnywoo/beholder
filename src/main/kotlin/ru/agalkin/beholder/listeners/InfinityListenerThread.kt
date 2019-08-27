package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.stats.Stats
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

class InfinityListenerThread(
    private val app: Beholder,
    private val infinityListener: InfinityListener,
    messageLengthBytes: Int,
    private val queue: MessageQueue
) : Thread("from-infinity-listener") {
    private val payload = "a".repeat(messageLengthBytes).toByteArray()

    override fun run() {
        InternalLog.info("Thread $name was started")

        while (!infinityListener.isListenerDeleted.get()) {
            val message = Message()

            message.setFieldValue("payload", FieldValue.fromByteArray(payload, payload.size))

            message["date"] = app.curDateIso()
            message["from"] = "beholder://infinity"

            queue.add(message)

//            Stats.reportUdpReceived(packet.length.toLong())
        }

        InternalLog.info("Thread $name was stopped")
    }
}
