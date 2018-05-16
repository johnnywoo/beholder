package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
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
    override fun formatMessage(message: Message): FieldValue {
        val payload = message.getFieldValue("payload")
        val byteLength = payload.getByteLength()

        return payload.prepend("$byteLength ")
    }
}
