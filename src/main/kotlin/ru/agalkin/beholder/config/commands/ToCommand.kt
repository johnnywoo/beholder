package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.parser.ArgumentToken

class ToCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    private val destination: Destination

    init {
        val usage = "Usage:\n" +
            "to stdout\n"

        try {
            destination = when (requireArg(1, usage)) {
                "stdout" -> StdoutDestination()
                else     -> throw CommandException(usage)
            }
        } catch (e: Address.AddressException) {
            throw CommandException(e)
        }
    }

    override fun emit(message: Message) {
        destination.write(message)

        super.emit(message)
    }

    interface Destination {
        fun write(message: Message)
    }

    class StdoutDestination : Destination {
        override fun write(message: Message) {
            println(message.text)
        }
    }
}
