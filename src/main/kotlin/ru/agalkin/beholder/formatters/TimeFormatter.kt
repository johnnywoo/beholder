package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import java.text.SimpleDateFormat
import java.util.*

class TimeFormatter : Formatter {
    override fun formatMessage(message: Message): String {
        return SimpleDateFormat("HH:mm:ss").format(Date())
    }
}
