package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import java.net.InetAddress

class HostFormatter : Formatter {
    private val hostValue = FieldValue.fromString(
        InetAddress.getLocalHost().hostName ?: "unknown-hostname"
    )

    override fun formatMessage(message: Message)
        = hostValue
}
