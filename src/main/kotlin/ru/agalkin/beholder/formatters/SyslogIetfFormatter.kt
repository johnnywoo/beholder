package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.getIsoDateFormatter
import java.net.InetAddress
import java.util.*

/**
 * IETF syslog format with timezone in date
 */
class SyslogIetfFormatter : Formatter {
    // хост сам перезагрузится на SIGHUP, потому что мы пересоздадим все команды и их внутренности
    private val defaultHost = InetAddress.getLocalHost().hostName

    override fun formatMessage(message: Message): String {
        // <15>1 2017-03-03T09:26:44+00:00 sender-host program-name - - -
        val sb = StringBuilder()

        val facility = message.getIntField("facility", 1) // 1 = user
        val severity = message.getIntField("severity", 6) // 6 = info

        // header
        sb.append("<").append(facility * 8 + severity).append(">1 ")

        // time (received time for now)
        val date = message.getDateField("date") ?: Date()
        sb.append(formatDate(date)).append(' ')

        // host
        sb.append(message.getStringField("host", defaultHost)).append(' ')

        // program name
        sb.append(message.getStringField("program", "-")).append(' ')

        // pid
        sb.append(message.getStringField("pid", "-")).append(' ')

        // message id, structured data
        sb.append("- - ")

        // payload
        sb.append(message.getPayload())

        return sb.toString()
    }

    private val dateFormat = getIsoDateFormatter()

    // чтобы всё было как можно более одинаковое, мы заменим Z для UTC на +00:00
    private fun formatDate(date: Date): String
        = dateFormat.format(date).replace(Regex("Z$"), "+00:00")
}
