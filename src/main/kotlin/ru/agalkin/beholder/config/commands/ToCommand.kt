package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ArgumentToken

class ToCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    private val destination: Destination
    private val format: Format

    init {
        val args = Args(arguments)

        val destinationName = args.shift("Destination type was not specified")
        destination = when (destinationName) {
            "stdout" -> StdoutDestination()
            else     -> throw CommandException("Unsupported destination type: $destinationName")
        }

        format = when (args.shiftIfPrefixed("as", "`from ... as` needs a format definition")) {
            "dump"    -> Dump()
            "payload" -> Payload()
            null      -> Payload()
            else      -> throw CommandException("Cannot understand arguments of `to` command")
        }

        args.end()
    }

    override fun emit(message: Message) {
        destination.write(format.formatMessage(message))

        super.emit(message)
    }


    interface Destination {
        fun write(string: String)
    }

    inner class StdoutDestination : Destination {
        override fun write(string: String) {
            println(string)
        }
    }


    interface Format {
        fun formatMessage(message: Message): String
    }

    class Dump : Format {
        override fun formatMessage(message: Message): String {
            val sb = StringBuilder(message.text)
            for ((tag, value) in message.tags) {
                sb.append("\n~").append(tag).append('=').append(value)
            }
            return sb.toString()
        }
    }

    class Payload : Format {
        override fun formatMessage(message: Message): String
            = message.getPayload()
    }


    // TODO надо эту штуку узаконить для всех команд и допилить получше
    private class Args(private val args: List<ArgumentToken>) {
        private var index = 0

        fun shiftIfPrefixed(prefixWord: String, errorMessage: String): String? {
            if (args.indices.contains(index + 1) && args[index + 1].getValue() == prefixWord) {
                index++
                return shift(errorMessage)
            }
            return null
        }

        fun shift(errorMessage: String): String {
            if (args.indices.contains(index + 1)) {
                index++
                return args[index].getValue()
            }
            throw CommandException(errorMessage)
        }

        fun end() {
            if (args.indices.contains(index + 1)) {
                throw CommandException("Too many arguments for `${args[0]}`")
            }
        }
    }
}
