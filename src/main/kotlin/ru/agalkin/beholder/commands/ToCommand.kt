package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.addNewlineIfNeeded
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.formatters.InterpolateStringFormatter
import ru.agalkin.beholder.threads.FileSender
import ru.agalkin.beholder.threads.TcpSender
import ru.agalkin.beholder.threads.UdpSender

class ToCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |to stdout;
            |to file <file>;
            |to udp [<address>:]<port>;
            |to tcp [<address>:]<port>;
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
            |A newline is appended unless the payload already ends with a newline.
            |
            |`to file <file>` stores payloads of messages into a file.
            |A newline is appended unless the payload already ends with a newline.
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
            |Default address is 127.0.0.1.
            |
            |`to tcp [<address>:]<port>` sends payloads of messages over a TCP connection.
            |Default address is 127.0.0.1.
            |A newline is appended unless the payload already ends with a newline.
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
                "tcp"    -> TcpDestination(Address.fromString(arguments.shiftString("`to tcp` needs at least a port number"), "127.0.0.1"))
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

    override fun start() {
        destination.start()
        super.start()
    }

    override fun stop() {
        destination.stop()
        super.stop()
    }


    private interface Destination {
        fun write(message: Message)

        fun start() {}
        fun stop() {}
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

    private class TcpDestination(address: Address) : Destination {
        private val sender = TcpSender.getSender(address)

        override fun write(message: Message) {
            sender.writeMessagePayload(addNewlineIfNeeded(message.getPayload()))
        }

        override fun start() {
            sender.incrementReferenceCount()
        }

        override fun stop() {
            sender.decrementReferenceCount()
        }
    }
}
