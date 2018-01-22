package ru.agalkin.beholder.senders

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.LinkedBlockingQueue

class UdpWriterThread(private val address: Address) : Thread("udp-writer-$address") {
    val queue = LinkedBlockingQueue<String>()

    override fun run() {
        InternalLog.info("Thread $name got started")

        var socket = DatagramSocket()
        val inetAddress = address.getInetAddress()

        while (true) {
            val text = queue.take() // blocking
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