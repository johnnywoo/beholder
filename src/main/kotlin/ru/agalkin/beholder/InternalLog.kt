package ru.agalkin.beholder

import ru.agalkin.beholder.threads.InternalLogListener
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

const val INTERNAL_LOG_FROM_FIELD = "beholder://internal-log"

class InternalLog {
    companion object {
        private var isWritingToStdoutAllowed = true

        fun stopWritingToStdout() {
            isWritingToStdoutAllowed = false
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

            val date = Date()

            val destination = when (Severity.WARNING.isMoreUrgentThan(severity)) {
                true  -> System.out
                false -> System.err
            }
            if (destination !== System.out || isWritingToStdoutAllowed) {
                destination.println(
                    SimpleDateFormat("HH:mm:ss").format(date)
                        + " " + text
                )
            }

            logFile?.appendText(
                (getIsoDate(date))
                    + " " + severity.name
                    + " " + addNewlineIfNeeded(text)
            )

            val message = Message()

            message["receivedDate"]   = getIsoDate()
            message["from"]           = INTERNAL_LOG_FROM_FIELD
            message["payload"]        = text
            message["syslogProgram"]  = BEHOLDER_SYSLOG_PROGRAM
            message["syslogSeverity"] = severity.getNumberAsString()

            InternalLogListener.add(message)
        }
    }
}
