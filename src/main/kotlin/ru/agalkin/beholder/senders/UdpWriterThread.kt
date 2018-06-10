package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.DataQueue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean

class UdpWriterThread(app: Beholder, private val address: Address) : Thread("udp-writer-$address") {
    val queue = DataQueue(app, ConfigOption.TO_UDP_BUFFER_MESSAGES_COUNT)

    val isRunning = AtomicBoolean(true)

    override fun run() {
        InternalLog.info("Thread $name got started")

        var socket = DatagramSocket()
        val inetAddress = address.getInetAddress()

        while (isRunning.get()) {
            val fieldValue = queue.shift(100) // blocking
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
