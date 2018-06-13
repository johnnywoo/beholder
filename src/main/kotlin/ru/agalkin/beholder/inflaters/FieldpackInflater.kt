package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message

class FieldpackInflater : Inflater {
    private val fieldpack = Fieldpack()

    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        try {
            val data = message.getPayloadValue().toByteArray()
            var index = 0
            val messages = fieldpack.readMessages { length ->
                val chunk = Fieldpack.Portion(data, index, length)
                index += length
                chunk
            }
            for (unpackedMessage in messages) {
                emit(unpackedMessage)
            }

            return true
        } catch (e: Throwable) {
            return false
        }
    }
}
