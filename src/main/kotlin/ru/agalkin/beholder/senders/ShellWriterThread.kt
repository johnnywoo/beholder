package ru.agalkin.beholder.senders

import ru.agalkin.beholder.*
import ru.agalkin.beholder.config.ConfigOption
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ShellWriterThread(app: Beholder, private val shellCommand: String) : Thread("shell-writer-${getCommandDescription(shellCommand)}") {
    val queue = DataQueue(app, ConfigOption.TO_SHELL_BUFFER_MESSAGES_COUNT)

    val isWriterDestroyed = AtomicBoolean(false)

    override fun run() {
        InternalLog.info("Thread $name $id got started")

        while (!isWriterDestroyed.get()) {
            try {
                startProcessAndLoop()
            } catch (e: Throwable) {
                InternalLog.err("$name ${e::class.simpleName} ${e.message}")
            }
        }

        InternalLog.info("Thread $name $id got stopped")
    }

    private val nextRestartAtMillis = AtomicLong()

    private fun startProcess(): Process? {
        while (System.currentTimeMillis() < nextRestartAtMillis.get()) {
            sleep(50)
        }
        nextRestartAtMillis.set(System.currentTimeMillis() + 1000)

        if (isWriterDestroyed.get()) {
            // запущенного процесса нет, коннект ушёл из конфига
            return null
        }

        val cwd = System.getProperty("user.dir")

        InternalLog.info("Executing shell command: $shellCommand")
        InternalLog.info("Current workdir: $cwd")

        val process = ProcessBuilder(shellCommand)
            .directory(File(cwd))
            .redirectErrorStream(true)
            .start()

        InternalLog.info("Started shell process ${process.pid()}")

        return process
    }

    private var undeliveredText: FieldValue? = null

    private fun startProcessAndLoop() {
        val process = startProcess()
        if (process == null) {
            return
        }

        readInputStreamAndDiscard(process.inputStream, "$name-skipper")

        process.outputStream.use { outputStream ->
            while (true) {
                val fieldValue = undeliveredText ?: queue.shift(100) // blocking for 100 millis
                if (fieldValue == null) {
                    // за 100 мс ничего не нашли
                    // проверим isWriterDestroyed и поедем ждать заново
                    if (isWriterDestroyed.get()) {
                        // очередь закончилась и коннект больше не нужен
                        break // ends connectAndLoop()
                    }
                    continue
                }

                // если в процессе отправки сообщения будет проблема (исключение),
                // то в следующий раз надо не брать сообщение из очереди,
                // а пытаться отправить то же самое второй раз
                undeliveredText = fieldValue

                outputStream.write(fieldValue.toByteArray(), 0, fieldValue.getByteLength())
                outputStream.flush()

                undeliveredText = null
            }
        }
    }

    companion object {
        private fun getCommandDescription(shellCommand: String)
            = substringUpTo(shellCommand.replace(Regex("[^a-zA-Z0-9]+"), ""), 20)
    }
}
