package ru.agalkin.beholder.threads

import ru.agalkin.beholder.InternalLog
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FileWriterThread(private val file: File) : Thread("file-writer-${file.name}") {
    val isWriterStopped = AtomicBoolean(false)
    val isReloadNeeded  = AtomicBoolean(false)

    val queue = LinkedBlockingQueue<String>()

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

            val text = queue.poll(100, TimeUnit.MILLISECONDS) // blocking for 100 millis
            if (text == null) {
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
                writer?.write(text)
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