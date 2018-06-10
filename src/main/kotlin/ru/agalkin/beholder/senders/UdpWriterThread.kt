package ru.agalkin.beholder.senders

import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.DataQueue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import java.net.DatagramPacket
import java.net.DatagramSocket

class UdpWriterThread(private val address: Address) : Thread("udp-writer-$address") {
    val queue = DataQueue(ConfigOption.TO_UDP_BUFFER_MESSAGES_COUNT)

    override fun run() {
        InternalLog.info("Thread $name got started")

        var socket = DatagramSocket()
        val inetAddress = address.getInetAddress()

        while (true) {
            val fieldValue = queue.shift(1000) // blocking
            if (fieldValue == null) {
                // почему-то ничего не нашли, надо ждать заново
                continue
            }

            try {
                val byteArray = fieldValue.toByteArray()
                socket.send(DatagramPacket(byteArray, fieldValue.getByteLength(), inetAddress, address.port))
            } catch (e: Throwable) {
                InternalLog.exception(e)
                socket.use { it.close() }
                socket = DatagramSocket()
            }
        }
    }
}
