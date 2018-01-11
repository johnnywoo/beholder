package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.addNewlineIfNeeded
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.formatters.TemplateFormatter
import ru.agalkin.beholder.threads.FileSender
import ru.agalkin.beholder.threads.TcpSender
import ru.agalkin.beholder.threads.UdpSender

class ToCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
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
        private val filenameFormatter = TemplateFormatter.create(filenameTemplate)

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
