package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class DumpFormatter : Formatter {
    override fun formatMessage(message: Message): String {
        val sb = StringBuilder()
        for ((tag, value) in message.getTags()) {
            if (!sb.isEmpty()) {
                sb.append("\n")
            }
            sb.append(tag).append('=').append(value)
        }
        return sb.toString()
    }
}
