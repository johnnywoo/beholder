package ru.agalkin.beholder.threads

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.Address
import java.util.concurrent.ConcurrentHashMap

const val TO_TCP_CONNECT_TIMEOUT_MILLIS = 2000
const val TO_TCP_MAX_BUFFER_COUNT = 1000 // string payloads

class TcpSender(address: Address) {
    private val writerThread = TcpWriterThread(address)

    fun writeMessagePayload(text: String) {
        // не даём очереди бесконтрольно расти (вытесняем старые записи)
        while (writerThread.queue.size > TO_TCP_MAX_BUFFER_COUNT) {
            writerThread.queue.take() // FIFO
        }
        writerThread.queue.offer(text)
    }

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before() {
                writerThread.isWriterPaused.set(true)
            }

            override fun after() {
                writerThread.isWriterPaused.set(false)

                // Пока система работает, она пытается переподключить тухлое соединение
                // с нарастающим интервалом.
                // После перезагрузки конфига надо сразу пробовать переподключиться заново.
                writerThread.reconnectIntervalSeconds.set(0)

                // config reload is finished, our sender has no references
                // this means we should drop the connection
                if (referenceCount == 0) {
                    writerThread.isWriterDestroyed.set(true)
                    synchronized(senders) {
                        senders.remove(address)
                    }
                }
            }
        })

        writerThread.start()
    }

    private var referenceCount = 0

    fun incrementReferenceCount() {
        referenceCount++
    }

    fun decrementReferenceCount() {
        referenceCount--
    }

    companion object {
        private val senders = ConcurrentHashMap<Address, TcpSender>()

        fun getSender(address: Address): TcpSender {
            val sender = senders[address]
            if (sender != null) {
                return sender
            }
            synchronized(senders) {
                val newSender = senders[address] ?: TcpSender(address)
                senders[address] = newSender
                return newSender
            }
        }
    }
}
