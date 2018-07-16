package ru.agalkin.beholder

import ru.agalkin.beholder.formatters.TimeFormatter
import ru.agalkin.beholder.listeners.InternalLogListener
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime

object InternalLog {
    private var stdout: PrintStream? = System.out
    private var stderr: PrintStream? = System.err

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

    val listeners = mutableSetOf<InternalLogListener>()

    private fun dispatchMessage(text: String?, severity: Severity) {
        if (text == null) {
            return
        }

        val date    = ZonedDateTime.now()
        val isoDate = TimeFormatter.FORMAT_STABLE_DATETIME.format(date)

        val destination = when (Severity.WARNING.isMoreUrgentThan(severity)) {
            true  -> stdout
            false -> stderr
        }
        destination?.println(
            TimeFormatter.FORMAT_TIME.format(date)
                + " " + text
        )

        logFile?.appendText(
            isoDate
                + " " + severity.name
                + " " + addNewlineIfNeeded(text)
        )

        val message = Message()

        message["date"]     = isoDate
        message["from"]     = "beholder://internal-log"
        message["payload"]  = text
        message["program"]  = BEHOLDER_SYSLOG_PROGRAM
        message["severity"] = severity.getNumberAsString()

        for (listener in listeners) {
            listener.add(message)
        }
    }
}
