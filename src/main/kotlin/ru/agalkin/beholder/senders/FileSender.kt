package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FileSender(app: Beholder, file: File) {
    private val fileThread = FileWriterThread(app, file)

    fun writeMessagePayload(value: FieldValue) {
        fileThread.queue.add(value)
    }

    init {
        app.beforeReloadCallbacks.add {
            fileThread.isWriterStopped.set(true)
            fileThread.isReloadNeeded.set(true)
        }

        app.afterReloadCallbacks.add {
            fileThread.isWriterStopped.set(false)
        }

        fileThread.start()
    }

    fun destroy() {
        fileThread.isWriterStopped.set(true)
    }

    class Factory(private val app: Beholder) {
        private val senders = ConcurrentHashMap<String, FileSender>()

        fun getSender(filename: String): FileSender {
            val canonicalPath = File(filename).canonicalPath
            val sender = senders[canonicalPath]
            if (sender != null) {
                return sender
            }
            synchronized(senders) {
                val newSender = senders[canonicalPath] ?: FileSender(app, File(canonicalPath))
                senders[canonicalPath] = newSender
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
