package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.queue.BeholderQueue
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentHashMap

class UdpSender(app: Beholder, address: Address) {
    private var socket = DatagramSocket()
    private val inetAddress = address.getInetAddress()

    private val queue = BeholderQueue<FieldValue>(app, ConfigOption.TO_UDP_BUFFER_MESSAGES_COUNT) { fieldValue ->
        try {
            val byteArray = fieldValue.toByteArray()
            socket.send(DatagramPacket(byteArray, fieldValue.getByteLength(), inetAddress, address.port))
        } catch (e: Throwable) {
            InternalLog.exception(e)
            socket.use { it.close() }
            socket = DatagramSocket()
        }
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        queue.add(fieldValue)
    }

    class Factory(private val app: Beholder) {
        private val senders = ConcurrentHashMap<Address, UdpSender>()

        fun getSender(address: Address): UdpSender {
            val sender = senders[address]
            if (sender != null) {
                return sender
            }
            synchronized(senders) {
                val newSender = senders[address] ?: UdpSender(app, address)
                senders[address] = newSender
                return newSender
            }
        }
    }
}
