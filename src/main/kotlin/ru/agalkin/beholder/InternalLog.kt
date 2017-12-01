package ru.agalkin.beholder

import org.apache.commons.lang3.exception.ExceptionUtils
import ru.agalkin.beholder.listeners.InternalLogListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

        fun exception(e: Throwable)
            = dispatchMessage(
                ExceptionUtils.getRootCauseStackTrace(e).joinToString("\n") { it },
                Severity.ERROR
            )

        const val FROM_FIELD_VALUE = "beholder://internal-log"

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
                    + " " + text
                    + "\n"
            )

            // мы не хотим раньше времени стартовать все подряд треды какие только есть
            // поэтому до реального появления в конфиге подписчиков на наш лог ничего стартовать не будем
            if (InternalLogListener.isInitialized) {
                val message = Message()

                message["receivedDate"]   = getIsoDate()
                message["from"]           = FROM_FIELD_VALUE
                message["payload"]        = text
                message["syslogProgram"]  = "beholder"
                message["syslogSeverity"] = severity.toString()

                InternalLogListener.instance.add(message)
            }
        }
    }
}
