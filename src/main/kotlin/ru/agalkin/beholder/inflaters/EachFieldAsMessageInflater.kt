package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

class EachFieldAsMessageInflater(
    private val keyField: String,
    private val valueField: String
) : Inflater {

    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        for (field in message.getFieldNames()) {
            emit(Message(hashMapOf(
                keyField to FieldValue.fromString(field),
                valueField to message.getFieldValue(field)
            )))
        }

        return true
    }
}
