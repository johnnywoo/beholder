package ru.agalkin.beholder.threads

import ru.agalkin.beholder.Beholder
import java.io.File
import java.util.concurrent.ConcurrentHashMap

const val TO_FILE_MAX_BUFFER_COUNT = 1000 // string payloads

class FileSender(file: File) {
    private val fileThread = FileWriterThread(file)

    fun writeMessagePayload(text: String) {
        // не даём очереди бесконтрольно расти (вытесняем старые записи)
        while (fileThread.queue.size > TO_FILE_MAX_BUFFER_COUNT) {
            fileThread.queue.take() // FIFO
        }
        fileThread.queue.offer(text)
    }

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before() {
                fileThread.isWriterStopped.set(true)
                fileThread.isReloadNeeded.set(true)
            }

            override fun after()
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
