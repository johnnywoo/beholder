package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ArgumentToken

class ToCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    private val destination: Destination
    private val format: Format

    init {
        val usage = """
            |Usage:
            |to <destination> [as <format>]
            |
            |Destinations:
            |  stdout
            |
            |Formats:
            |  payload (default)  -- detected payload string or just full message text
            |  dump  -- full message dump with all tags
        """.trimMargin()

        val args = Args(arguments)

        destination = when (args.shift(usage)) {
            "stdout" -> StdoutDestination()
            else     -> throw CommandException(usage)
        }

        format = when (args.shiftIfPrefixed("as", usage)) {
            "dump"    -> Dump()
            "payload" -> Payload()
            else      -> throw CommandException(usage)
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
