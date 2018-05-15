package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

/**
 * RFC5425 message length prefix for TCP syslog
 *
 * All syslog messages MUST be sent as TLS "application data".  It is
 * possible for multiple syslog messages to be contained in one TLS
 * record or for a single syslog message to be transferred in multiple
 * TLS records.  The application data is defined with the following ABNF
 * expression:
 *  APPLICATION-DATA = 1*SYSLOG-FRAME
 *  SYSLOG-FRAME = MSG-LEN SP SYSLOG-MSG
 */
class SyslogFrameFormatter : Formatter {
    override fun formatMessage(message: Message): String {
        val payload = message.getPayload()
        val bytes = payload.toByteArray()

        val sb = StringBuilder()
        sb.append(bytes.size).append(' ').append(payload)

        return sb.toString()
    }
}
