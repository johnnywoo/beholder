package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message

interface Inflater {
    fun inflateMessageFields(message: Message): Boolean
}
