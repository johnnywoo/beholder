package ru.agalkin.beholder.senders

import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap

class UdpSender(address: Address) {
    private val writerThread = UdpWriterThread(address)
    init {
        writerThread.start()
    }

    fun writeMessagePayload(text: String) {
        writerThread.queue.add(text)
    }

    companion object {
        private val senders = ConcurrentHashMap<Address, UdpSender>()

        fun getSender(address: Address): UdpSender {
            val sender = senders[address]
            if (sender != null) {
                return sender
            }
            synchronized(senders) {
                val newSender = senders[address] ?: UdpSender(address)
                senders[address] = newSender
                return newSender
            }
        }
    }
}
