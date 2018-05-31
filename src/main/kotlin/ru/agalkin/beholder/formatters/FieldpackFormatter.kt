package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message

class FieldpackFormatter(private val fields: List<String>?) : Formatter {
    private val fieldpack = Fieldpack()

    override fun formatMessage(message: Message): FieldValue {
        val length: Int
        val buffer: ByteArray

        if (fields != null) {
            // For now, an inefficient but working method.
            // Fieldpack is not intended to be used on singular messages,
            // it needs to be paired with a buffer anyway.
            length = fieldpack.writeMessages(listOf(message), fields) { _, _ -> }
            buffer = ByteArray(length)
            var index = 0
            fieldpack.writeMessages(listOf(message), fields) { source, readLength ->
                for (i in 0 until readLength) {
                    buffer[index++] = source[i]
                }
            }
        } else {
            length = fieldpack.writeMessages(listOf(message)) { _, _ -> }
            buffer = ByteArray(length)
            var index = 0
            fieldpack.writeMessages(listOf(message)) { source, readLength ->
                for (i in 0 until readLength) {
                    buffer[index++] = source[i]
                }
            }

        }
        return FieldValue.fromByteArray(buffer, length)
    }
}
