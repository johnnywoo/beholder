package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.threadLocal

class FieldpackInflater : Inflater {
    private val fieldpack by threadLocal { Fieldpack() }

    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        try {
            val data = message.getPayloadValue().toByteArray()
            var index = 0
            val messages = fieldpack.readMessages { length ->
                val portion = Fieldpack.Portion(data, index, length)
                index += length
                portion
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
