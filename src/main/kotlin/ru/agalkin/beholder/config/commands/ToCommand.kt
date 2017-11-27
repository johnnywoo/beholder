package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message

class ToCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |to stdout;
            |
            |This command writes `¥payload` field of incoming messages to some destination.
            |To format the payload, use `set ¥payload ...` command.
            |
            |Example config:
            |  flow {
            |      from timer {set ¥payload '¥receivedDate Just a repeating text message'}
            |      to stdout;
            |  }
            |
            |This example config will produce messages like these:
            |  2017-11-27T21:14:01+03:00 Just a repeating text message
            |  2017-11-27T21:14:02+03:00 Just a repeating text message
            |  2017-11-27T21:14:03+03:00 Just a repeating text message
            |
            |Currently only writing to stdout is supported.
            |""".trimMargin().replace("¥", "$")
    }

    private val destination: Destination

    init {
        val destinationName = arguments.shift("Destination type was not specified")

        destination = when (destinationName) {
            "stdout" -> StdoutDestination()
            else     -> throw CommandException("Unsupported destination type: $destinationName")
        }

        arguments.end()
    }

    override fun emit(message: Message) {
        destination.write(message.getPayload())

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
}
