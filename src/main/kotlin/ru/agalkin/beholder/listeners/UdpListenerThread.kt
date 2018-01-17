package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.getIsoDateFormatter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.*

class UdpListenerThread(private val udpListener: UdpListener) : Thread("from-udp-${udpListener.address}-listener") {
    private val buffer = ByteArray(FROM_UDP_MAX_MESSAGE_CHARS)

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

                // не даём очереди бесконтрольно расти (вытесняем старые записи)
                if (udpListener.queue.size > FROM_UDP_MAX_BUFFER_COUNT) {
                    udpListener.queue.take() // FIFO
                }

                val message = Message()

                message["payload"]      = String(packet.data, 0, packet.length)
                message["receivedDate"] = curDateIso()
                message["from"]         = "udp://${packet.address.hostAddress}:${packet.port}"

                udpListener.queue.offer(message)

                UdpListener.updateMaxReceivedPacketSize(packet.length)
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
