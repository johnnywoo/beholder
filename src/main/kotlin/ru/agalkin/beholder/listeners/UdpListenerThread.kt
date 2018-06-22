package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.stats.Stats
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.*

class UdpListenerThread(
    private val udpListener: UdpListener,
    private val queue: MessageQueue
) : Thread("from-udp-${udpListener.address}-listener") {
    private val buffer = ByteArray(FROM_UDP_MAX_MESSAGE_BYTES)

    override fun run() {
        InternalLog.info("Thread $name got started")

        DatagramSocket(udpListener.address.port, udpListener.address.getInetAddress()).use { socket ->
            socket.soTimeout = 100 // millis

            // лисенер останавливается, когда подписчики кончились
            while (!udpListener.isListenerDeleted.get()) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    // 100 millis passed without any packet
                    // just loop around, check proper conditions and then receive again
                    continue
                }

                val message = Message()

                val byteArray = ByteArray(packet.length) { packet.data[it] }
                message.setFieldValue("payload", FieldValue.fromByteArray(byteArray, packet.length))

                message["date"] = curDateIso()
                message["from"] = "udp://${packet.address.hostAddress}:${packet.port}"

                queue.add(message)

                Stats.reportUdpReceived(packet.length.toLong())
            }
        }

        InternalLog.info("Thread $name got deleted")
    }

    // 2017-11-26T16:16:01+03:00
    // 2017-11-26T16:16:01Z if UTC
    private val formatter = getIsoDateFormatter()

    private fun curDateIso(): String
        = formatter.format(Date())
}
