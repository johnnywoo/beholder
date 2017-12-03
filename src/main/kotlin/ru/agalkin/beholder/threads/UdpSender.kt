package ru.agalkin.beholder.threads

import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap

class UdpSender(address: Address) {
    private val writerThread = UdpWriterThread(address)

    fun writeMessagePayload(text: String) {
        // не даём очереди бесконтрольно расти (вытесняем старые записи)
        while (writerThread.queue.size > MAX_BUFFER_COUNT) {
            writerThread.queue.take() // FIFO
        }
        writerThread.queue.offer(text)
    }

    init {
        writerThread.start()
    }

    companion object {
        val MAX_BUFFER_COUNT = 1000 // string payloads

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
