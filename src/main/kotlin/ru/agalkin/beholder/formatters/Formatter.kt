package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

interface Formatter {
    fun formatMessage(message: Message): FieldValue
}
