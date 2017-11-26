package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class PayloadFormatter : Formatter {
    override fun formatMessage(message: Message): String
        = message.getPayload()
}
