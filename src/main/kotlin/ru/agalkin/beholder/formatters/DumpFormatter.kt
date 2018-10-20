package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

class DumpFormatter(private val prefix: String? = null) : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        val sb = StringBuilder()
        sb.append(prefix ?: "").append('#').append(message.messageId)
        for (field in message.getFieldNames().sorted()) {
            sb.append('\n').append(prefix ?: "").append('$').append(field).append('=').append(message.getStringField(field))
        }
        return FieldValue.fromString(sb.toString())
    }
}
