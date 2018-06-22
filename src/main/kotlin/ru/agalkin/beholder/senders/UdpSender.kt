package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.queue.BeholderQueueAbstract
import ru.agalkin.beholder.queue.FieldValueQueue
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentHashMap

class UdpSender(app: Beholder, address: Address) {
    private var socket = DatagramSocket()
    private val inetAddress = address.getInetAddress()

    private val queue = FieldValueQueue(app) { fieldValue ->
        try {
            socket.send(DatagramPacket(
                fieldValue.toByteArray(),
                fieldValue.getByteLength(),
                inetAddress,
                address.port
            ))
        } catch (e: Throwable) {
            InternalLog.exception(e)
            socket.use { it.close() }
            socket = DatagramSocket()
        }
        BeholderQueueAbstract.Result.OK
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
