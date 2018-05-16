package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

class DumpFormatter : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        val sb = StringBuilder()
        sb.append('#').append(message.messageId)
        for (field in message.getFieldNames()) {
            sb.append("\n$").append(field).append('=').append(message.getStringField(field))
        }
        return FieldValue.fromString(sb.toString())
    }
}
