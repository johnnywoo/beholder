package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap

class UdpSender(app: Beholder, address: Address) {
    private val writerThread = UdpWriterThread(app, address)
    init {
        writerThread.start()
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        writerThread.queue.add(fieldValue)
    }

    fun destroy() {
        writerThread.isRunning.set(false)
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

        fun destroyAllSenders(): Int {
            val n = senders.size
            for (sender in senders.values) {
                sender.destroy()
            }
            return n
        }
    }
}
