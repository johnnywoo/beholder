package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import java.util.concurrent.CopyOnWriteArraySet

class ShellSender(app: Beholder, shellCommand: String) {
    private val writerThread = ShellWriterThread(app, shellCommand)
    init {
        writerThread.start()
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        writerThread.queue.add(fieldValue)
    }

    fun stop() {
        writerThread.isWriterDestroyed.set(true)
    }

    class Factory(private val app: Beholder) {
        private val senders = CopyOnWriteArraySet<ShellSender>()

        fun createSender(shellCommand: String): ShellSender {
            val shellSender = ShellSender(app, shellCommand)
            senders.add(shellSender)
            return shellSender
        }

        fun destroyAllSenders(): Int {
            val n = senders.size
            for (sender in senders) {
                sender.stop()
            }
            return n
        }
    }
}
