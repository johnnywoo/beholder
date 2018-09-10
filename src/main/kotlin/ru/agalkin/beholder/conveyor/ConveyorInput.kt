package ru.agalkin.beholder.conveyor

import ru.agalkin.beholder.Message

interface ConveyorInput {
    fun addMessage(message: Message)
}
