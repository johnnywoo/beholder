package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract

class DropCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    init {
        arguments.end()
    }

    override fun input(message: Message) {
        // do not send the message further = drop the message
    }
}
