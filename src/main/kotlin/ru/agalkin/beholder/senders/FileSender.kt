package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.queue.FieldValueQueue
import ru.agalkin.beholder.queue.Received
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class FileSender(app: Beholder, private val file: File) {
    private val isReloadNeeded = AtomicBoolean(false)
    init {
        app.afterReloadCallbacks.add {
            isReloadNeeded.set(true)
        }
    }

    private val queue = FieldValueQueue(app) { fieldValue ->
        try {
            if (isReloadNeeded.get()) {
                restartWriter()
            }

            val writer = getWriter()
            writer?.write(fieldValue.toString())
            writer?.flush()

            if (writer == null) {
                InternalLog.err("Skipped writing to $file")
            }
        } catch (e: Throwable) {
            InternalLog.exception(e)
            restartWriter()
        }
        Received.OK
    }

    private var bufferedWriter: BufferedWriter? = null

    private fun getWriter(): BufferedWriter? {
        val writer = bufferedWriter
        if (writer != null) {
            return writer
        }

        restartWriter()
        return bufferedWriter
    }

    private fun restartWriter(): Boolean {
        isReloadNeeded.set(false)

        val writer = bufferedWriter
        if (writer != null) {
            InternalLog.info("Closing file writer: $file")
            writer.close()
            bufferedWriter = null
        }

        try {
            file.parentFile.mkdirs()
            if (!file.exists()) {
                InternalLog.info("Creating file: $file")
                file.createNewFile()
            }
            if (!file.exists()) {
                InternalLog.err("Cannot locate file: $file")
                return false
            }
            if (!file.isFile) {
                InternalLog.err("Not a file: $file")
                return false
            }
            if (!file.canWrite()) {
                InternalLog.err("Cannot write to a file: $file")
                return false
            }

            InternalLog.info("Starting file writer: $file")
            bufferedWriter = BufferedWriter(FileWriter(file, true))

            return true
        } catch (e: Throwable) {
            InternalLog.err("Cannot use file for output: ${e::class.simpleName} ${e.message}")
            return false
        }
    }

    fun writeMessagePayload(value: FieldValue) {
        queue.add(value)
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
    }
}
