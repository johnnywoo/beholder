package ru.agalkin.beholder.senders

import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.DataQueue
import ru.agalkin.beholder.InternalLog
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean

class FileWriterThread(private val file: File) : Thread("file-writer-${file.name}") {
    val isWriterStopped = AtomicBoolean(false)
    val isReloadNeeded  = AtomicBoolean(false)

    val queue = DataQueue(ConfigOption.TO_FILE_BUFFER_MESSAGES_COUNT)

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

            bufferedWriter = BufferedWriter(FileWriter(file, true))

            return true
        } catch (e: Throwable) {
            InternalLog.err("Cannot use file for output: ${e::class.simpleName} ${e.message}")
            return false
        }
    }

    override fun run() {
        InternalLog.info("Thread $name got started")

        while (true) {
            if (isReloadNeeded.get()) {
                restartWriter()
            }

            while (isWriterStopped.get()) {
                Thread.sleep(50)
            }

            val fieldValue = queue.shift(100) // blocking for 100 millis
            if (fieldValue == null) {
                // за 100 мс ничего не нашли
                // проверим все условия и поедем ждать заново
                continue
            }

            while (isWriterStopped.get()) {
                Thread.sleep(50)
            }

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
        }
    }
}
