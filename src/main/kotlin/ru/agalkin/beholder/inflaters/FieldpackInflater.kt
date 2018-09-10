package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message

class FieldpackInflater : Inflater {
    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        try {
            val data = message.getPayloadValue().toByteArray()
            val messages = Fieldpack.readMessagesFromByteArray(data)
            for (unpackedMessage in messages) {
                emit(unpackedMessage)
            }

            return true
        } catch (e: Throwable) {
            InternalLog.exception(e)
            return false
        }
    }
}
