package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

/**
 * IETF syslog format with timezone in date
 */
class SyslogIetfFormatter : Formatter {
    private val defaultHost = InetAddress.getLocalHost().hostName

    override fun formatMessage(message: Message): String {
        // <15>1 2017-03-03T09:26:44+00:00 sender-host program-name - - -
        val sb = StringBuilder()

        val facility = message.intTag("syslogFacility", 1) // 1 = user
        val severity = message.intTag("syslogSeverity", 6) // 6 = info

        // header
        sb.append("<").append(facility * 8 + severity).append(">1 ")

        // time (received time for now)
        val date = message.dateTag("receivedDate") ?: Date()
        sb.append(formatDate(date)).append(' ')

        // host
        sb.append(message.stringTag("syslogHost") ?: defaultHost).append(' ')

        // program name
        sb.append(message.stringTag("syslogTag") ?: "-").append(' ')

        // pid, message id, structured data
        sb.append("- - - ")

        // payload
        sb.append(message.getPayload())

        return sb.toString()
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

    // чтобы всё было как можно более одинаковое, мы заменим Z для UTC на +00:00
    private fun formatDate(date: Date): String
        = dateFormat.format(date).replace(Regex("Z$"), "+00:00")
}
