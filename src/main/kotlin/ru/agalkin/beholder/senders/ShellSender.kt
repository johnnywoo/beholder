package ru.agalkin.beholder.senders

import java.util.concurrent.CopyOnWriteArraySet

class ShellSender(shellCommand: String) {
    private val writerThread = ShellWriterThread(shellCommand)
    init {
        writerThread.start()
    }

    fun writeMessagePayload(text: String) {
        // не даём очереди бесконтрольно расти (вытесняем старые записи)
        while (writerThread.queue.size > TO_UDP_MAX_BUFFER_COUNT) {
            writerThread.queue.take() // FIFO
        }
        writerThread.queue.offer(text)
    }

    fun stop() {
        writerThread.isWriterDestroyed.set(true)
    }

    companion object {
        private val senders = CopyOnWriteArraySet<ShellSender>()

        fun createSender(shellCommand: String): ShellSender {
            val shellSender = ShellSender(shellCommand)
            senders.add(shellSender)
            return shellSender
        }
    }
}
