package ru.agalkin.beholder

import ru.agalkin.beholder.formatters.TimeFormatter
import ru.agalkin.beholder.listeners.InternalLogListener
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference

object InternalLog {
    private var stdout: PrintStream? = System.out
    private var stderr: PrintStream? = System.err

    val lastAppInstance = AtomicReference<Beholder>()

    fun setStdout(printStream: PrintStream?) {
        stdout = printStream
    }

    fun setStderr(printStream: PrintStream?) {
        stderr = printStream
    }

    private var logFile: File? = null

    fun copyToFile(filename: String) {
        val file = File(filename)
        if (!file.isFile) {
            file.createNewFile()
        }
        logFile = file
    }

    fun info(text: String?)
        = dispatchMessage(text, Severity.INFO)

    fun err(text: String?)
        = dispatchMessage(text, Severity.ERROR)

    fun exception(e: Throwable) {
        val writer = StringWriter()
        e.printStackTrace(PrintWriter(writer))
        dispatchMessage(
            writer.buffer.toString(),
            Severity.ERROR
        )
    }

    val listeners = CopyOnWriteArraySet<InternalLogListener>()

    private fun dispatchMessage(text: String?, severity: Severity) {
        if (text == null) {
            return
        }

        // This date goes into stdout and file log.
        // App config may not exist yet, so we have to use system timezone here.
        val dateForRawLogging = ZonedDateTime.now()

        // This date goes into messages (`from internal-log`),
        // so we must respect timezone preferences from the config.
        val isoDateForMessage: String
        val app = lastAppInstance.get()
        if (app != null) {
            isoDateForMessage = app.curDateIso()
        } else {
            isoDateForMessage = TimeFormatter.FORMAT_STABLE_DATETIME.format(ZonedDateTime.now())
        }

        val destination = when (Severity.WARNING.isMoreUrgentThan(severity)) {
            true  -> stdout
            false -> stderr
        }
        destination?.println(
            TimeFormatter.FORMAT_TIME.format(dateForRawLogging)
                + " " + text
        )

        logFile?.appendText(
            TimeFormatter.FORMAT_STABLE_DATETIME.format(dateForRawLogging)
                + " " + severity.name
                + " " + addNewlineIfNeeded(text)
        )

        val message = Message()

        message["date"]     = isoDateForMessage
        message["from"]     = "beholder://internal-log"
        message["payload"]  = text
        message["program"]  = BEHOLDER_SYSLOG_PROGRAM
        message["severity"] = severity.getNumberAsString()

        for (listener in listeners) {
            listener.add(message)
        }
    }
}
