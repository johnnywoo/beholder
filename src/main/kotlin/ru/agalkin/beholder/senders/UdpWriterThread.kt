package ru.agalkin.beholder.senders

import ru.agalkin.beholder.ConfigOption
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.StringQueue
import ru.agalkin.beholder.config.Address
import java.net.DatagramPacket
import java.net.DatagramSocket

class UdpWriterThread(private val address: Address) : Thread("udp-writer-$address") {
    val queue = StringQueue(ConfigOption.TO_UDP_BUFFER_MESSAGES_COUNT)

    override fun run() {
        InternalLog.info("Thread $name got started")

        var socket = DatagramSocket()
        val inetAddress = address.getInetAddress()

        while (true) {
            val text = queue.shift(1000) // blocking
            if (text == null) {
                // почему-то ничего не нашли, надо ждать заново
                continue
            }

            try {
                val byteArray = text.toByteArray()
                socket.send(DatagramPacket(byteArray, byteArray.size, inetAddress, address.port))
            } catch (e: Throwable) {
                InternalLog.exception(e)
                socket.use { it.close() }
                socket = DatagramSocket()
            }
        }
    }
}
