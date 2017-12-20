package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.addNewlineIfNeeded
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.formatters.InterpolateStringFormatter
import ru.agalkin.beholder.threads.FileSender
import ru.agalkin.beholder.threads.UdpSender

class ToCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |to stdout;
            |to file <file>;
            |to udp [<address>:]<port>;
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
            |`to stdout` simply sends payloads of messages into STDOUT of beholder process.
            |
            |`to file <file>` stores payloads of messages into a file.
            |Relative filenames are resolved from CWD of beholder process.
            |You can use message fields in filenames:
            |  flow {
            |      from udp 1234;
            |      parse syslog;
            |      set ¥payload syslog;
            |      to file '/var/log/export/¥syslogHost/¥syslogProgram.log';
            |  }
            |
            |`to udp [<address>:]<port>` sends payloads of messages as UDP packets.
            |""".trimMargin().replace("¥", "$")
    }

    private val destination: Destination

    init {
        val destinationName = arguments.shiftString("Destination type was not specified")

        try {
            destination = when (destinationName) {
                "stdout" -> StdoutDestination()
                "file"   -> FileDestination(arguments.shiftString("`to file` needs a filename"))
                "udp"    -> UdpDestination(Address.fromString(arguments.shiftString("`to udp` needs at least a port number"), "127.0.0.1"))
                else     -> throw CommandException("Unsupported destination type: $destinationName")
            }
        } catch (e: Address.AddressException) {
            throw CommandException(e).apply { addSuppressed(e) }
        }

        arguments.end()
    }

    override fun receiveMessage(message: Message) {
        destination.write(message)

        super.receiveMessage(message)
    }


    private interface Destination {
        fun write(message: Message)
    }

    private class StdoutDestination : Destination {
        override fun write(message: Message) {
            print(addNewlineIfNeeded(message.getPayload()))
        }
    }

    private class FileDestination(filenameTemplate: String) : Destination {
        private val filenameFormatter = InterpolateStringFormatter(filenameTemplate)

        override fun write(message: Message) {
            val sender = FileSender.getSender(filenameFormatter.formatMessage(message))
            sender.writeMessagePayload(addNewlineIfNeeded(message.getPayload()))
        }
    }

    private class UdpDestination(address: Address) : Destination {
        private val sender = UdpSender.getSender(address)

        override fun write(message: Message) {
            sender.writeMessagePayload(message.getPayload())
        }
    }
}
