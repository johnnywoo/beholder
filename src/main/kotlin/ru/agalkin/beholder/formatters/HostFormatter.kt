package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.getSystemHostname
import java.net.InetAddress

class HostFormatter : Formatter {
    private val hostValue = FieldValue.fromString(
        getSystemHostname() ?: "unknown-hostname"
    )

    override fun formatMessage(message: Message)
        = hostValue
}
