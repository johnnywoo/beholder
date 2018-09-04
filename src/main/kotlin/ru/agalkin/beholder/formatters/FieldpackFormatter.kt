package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message

class FieldpackFormatter(private val fields: List<String>?) : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        // For now, an inefficient but working method.
        // Fieldpack is not intended to be used on singular messages,
        // it needs to be paired with a buffer anyway.
        val buffer = when (fields) {
            null -> Fieldpack.writeMessagesToByteArray(listOf(message))
            else -> Fieldpack.writeMessagesToByteArray(listOf(message), fields)
        }
        return FieldValue.fromByteArray(buffer, buffer.size)
    }
}
