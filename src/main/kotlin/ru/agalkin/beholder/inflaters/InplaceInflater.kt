package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message

interface InplaceInflater : Inflater {
    fun inflateMessageFieldsInplace(message: Message): Boolean

    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        if (inflateMessageFieldsInplace(message)) {
            emit(message)
            return true
        }

        return false
    }
}
