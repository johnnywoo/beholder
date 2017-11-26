package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

interface Formatter {
    fun formatMessage(message: Message): String
}
