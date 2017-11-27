package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class DumpFormatter : Formatter {
    override fun formatMessage(message: Message): String {
        val sb = StringBuilder()
        sb.append('#').append(message.messageId)
        for ((field, value) in message.getFields()) {
            sb.append("\n$").append(field).append('=').append(value)
        }
        return sb.toString()
    }
}
