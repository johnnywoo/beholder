package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ArgumentToken

class ConvertCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    override fun emit(message: Message) {
        if (arguments.indices.contains(1)) {
            val mode = arguments[1].getValue()
            when (mode) {
                "up" -> super.emit(message.copy(text = message.text.toUpperCase()))
                else -> super.emit(message)
            }
        }
    }
}
