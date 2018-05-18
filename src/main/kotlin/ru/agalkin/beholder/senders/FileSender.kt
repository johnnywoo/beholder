package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FileSender(file: File) {
    private val fileThread = FileWriterThread(file)

    fun writeMessagePayload(value: FieldValue) {
        fileThread.queue.add(value)
    }

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before(app: Beholder) {
                fileThread.isWriterStopped.set(true)
                fileThread.isReloadNeeded.set(true)
            }

            override fun after(app: Beholder)
                = fileThread.isWriterStopped.set(false)
        })

        fileThread.start()
    }

    companion object {
        private val senders = ConcurrentHashMap<String, FileSender>()

        fun getSender(filename: String): FileSender {
            val canonicalPath = File(filename).canonicalPath
            val sender = senders[canonicalPath]
            if (sender != null) {
                return sender
            }
            synchronized(senders) {
                val newSender = senders[canonicalPath] ?: FileSender(File(canonicalPath))
                senders[canonicalPath] = newSender
                return newSender
            }
        }
    }
}
