package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.formatters.DumpFormatter
import ru.agalkin.beholder.formatters.Formatter
import ru.agalkin.beholder.formatters.PayloadFormatter
import ru.agalkin.beholder.formatters.SyslogIetfFormatter

class ToCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    private val destination: Destination
    private val formatter: Formatter

    init {
        val args = Args(arguments)

        val destinationName = args.shift("Destination type was not specified")
        destination = when (destinationName) {
            "stdout" -> StdoutDestination()
            else     -> throw CommandException("Unsupported destination type: $destinationName")
        }

        formatter = when (args.shiftIfPrefixed("as", "`from ... as` needs a format definition")) {
            "dump"    -> DumpFormatter()
            "syslog"  -> SyslogIetfFormatter()
            "payload" -> PayloadFormatter()
            null      -> PayloadFormatter()
            else      -> throw CommandException("Cannot understand arguments of `to` command")
        }

        args.end()
    }

    override fun emit(message: Message) {
        destination.write(formatter.formatMessage(message))

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
