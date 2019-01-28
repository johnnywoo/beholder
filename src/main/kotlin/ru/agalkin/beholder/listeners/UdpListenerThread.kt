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

class UdpListenerThread(
    private val app: Beholder,
    private val udpListener: UdpListener,
    private val queue: MessageQueue
) : Thread("from-udp-${udpListener.address}-listener") {
    private val buffer = ByteArray(FROM_UDP_MAX_MESSAGE_BYTES)

    override fun run() {
        InternalLog.info("Thread $name was started")

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

                message["date"] = app.curDateIso()
                message["from"] = "udp://${packet.address.hostAddress}:${packet.port}"

                queue.add(message)

                Stats.reportUdpReceived(packet.length.toLong())
            }
        }

        InternalLog.info("Thread $name was stopped")
    }
}
