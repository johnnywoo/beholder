package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.queue.BeholderQueue
import ru.agalkin.beholder.readInputStreamAndDiscard
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong

class ShellSender(app: Beholder, private val shellCommand: String) {
    val queue = BeholderQueue<FieldValue>(app) { fieldValue ->
        val process = startProcess()
        try {
            val outputStream = process.outputStream
            outputStream.write(fieldValue.toByteArray(), 0, fieldValue.getByteLength())
            outputStream.flush()
            BeholderQueue.Result.OK
        } catch (e: Throwable) {
            InternalLog.exception(e)
            BeholderQueue.Result.RETRY
        }
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        queue.add(fieldValue)
    }

    private val nextRestartAtMillis = AtomicLong()

    private var process: Process? = null

    private fun startProcess(): Process {
        val currentProcess = process
        if (currentProcess != null && currentProcess.isAlive) {
            return currentProcess
        }

        while (System.currentTimeMillis() < nextRestartAtMillis.get()) {
            Thread.sleep(50)
        }
        nextRestartAtMillis.set(System.currentTimeMillis() + 1000)

        val cwd = System.getProperty("user.dir")

        InternalLog.info("Executing shell command: $shellCommand")
        InternalLog.info("Current workdir: $cwd")

        val newProcess = ProcessBuilder(shellCommand)
            .directory(File(cwd))
            .redirectErrorStream(true)
            .start()

        InternalLog.info("Started shell process ${newProcess.pid()}")

        readInputStreamAndDiscard(newProcess.inputStream, "shell-skipper")

        process = newProcess
        return newProcess
    }

    class Factory(private val app: Beholder) {
        private val senders = CopyOnWriteArraySet<ShellSender>()

        fun createSender(shellCommand: String): ShellSender {
            val shellSender = ShellSender(app, shellCommand)
            senders.add(shellSender)
            return shellSender
        }
    }
}
