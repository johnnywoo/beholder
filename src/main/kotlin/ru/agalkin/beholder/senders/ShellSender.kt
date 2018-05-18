package ru.agalkin.beholder.senders

import ru.agalkin.beholder.FieldValue
import java.util.concurrent.CopyOnWriteArraySet

class ShellSender(shellCommand: String) {
    private val writerThread = ShellWriterThread(shellCommand)
    init {
        writerThread.start()
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        writerThread.queue.add(fieldValue)
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
