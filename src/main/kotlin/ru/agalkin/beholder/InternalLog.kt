package ru.agalkin.beholder

import ru.agalkin.beholder.listeners.InternalLogListener
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object InternalLog {
    private var allowRegularOutput = true
    private var allowErrorOutput   = true

    fun stopWritingToStdout() {
        allowRegularOutput = false
    }

    fun stopWritingToStderr() {
        allowErrorOutput = false
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

    private fun dispatchMessage(text: String?, severity: Severity) {
        if (text == null) {
            return
        }

        val date    = Date()
        val isoDate = getIsoDate(date)

        val destination = when (Severity.WARNING.isMoreUrgentThan(severity)) {
            true  -> if (allowRegularOutput) System.out else null
            false -> if (allowErrorOutput) System.err else null
        }
        destination?.println(
            SimpleDateFormat("HH:mm:ss").format(date)
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

        InternalLogListener.add(message)
    }
}
