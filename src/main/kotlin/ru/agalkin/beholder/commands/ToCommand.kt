package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.formatters.TemplateFormatter
import ru.agalkin.beholder.senders.FileSender
import ru.agalkin.beholder.senders.ShellSender
import ru.agalkin.beholder.senders.TcpSender
import ru.agalkin.beholder.senders.UdpSender

class ToCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    private val destination: Destination

    init {
        try {
            destination = when (arguments.shiftAnyLiteral("Destination type was not specified")) {
                "stdout" -> StdoutDestination(TemplateFormatter.payloadFormatter)

                "file" -> FileDestination(
                    arguments.shiftStringTemplate("`to file` needs a filename"),
                    TemplateFormatter.payloadFormatter
                )

                "udp" -> UdpDestination(
                    Address.fromString(arguments.shiftFixedString("`to udp` needs at least a port number"), "127.0.0.1"),
                    TemplateFormatter.payloadFormatter
                )

                "tcp" -> TcpDestination(
                    Address.fromString(arguments.shiftFixedString("`to tcp` needs at least a port number"), "127.0.0.1"),
                    TemplateFormatter.payloadFormatter
                )

                "shell" -> ShellDestination(
                    arguments.shiftFixedString("`to shell` needs a shell command"),
                    TemplateFormatter.payloadFormatter
                )

                else -> throw CommandException("Unsupported destination type")
            }
        } catch (e: Address.AddressException) {
            throw CommandException(e).apply { addSuppressed(e) }
        }

        arguments.end()
    }

    override fun input(message: Message) {
        destination.write(message)
        output.sendMessageToSubscribers(message)
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

    private class StdoutDestination(private val template: TemplateFormatter) : Destination {
        override fun write(message: Message) {
            print(template.formatMessage(message).withNewlineAtEnd().toString())
        }
    }

    private class FileDestination(
        private val filenameTemplate: TemplateFormatter,
        private val dataTemplate: TemplateFormatter
    ) : Destination {

        override fun write(message: Message) {
            val sender = FileSender.getSender(filenameTemplate.formatMessage(message).toString())
            sender.writeMessagePayload(dataTemplate.formatMessage(message).withNewlineAtEnd())
        }
    }

    private class UdpDestination(address: Address, private val template: TemplateFormatter) : Destination {
        private val sender = UdpSender.getSender(address)

        override fun write(message: Message) {
            sender.writeMessagePayload(template.formatMessage(message))
        }
    }

    private class TcpDestination(address: Address, private val template: TemplateFormatter) : Destination {
        private val sender = TcpSender.getSender(address)

        override fun write(message: Message) {
            sender.writeMessagePayload(template.formatMessage(message).withNewlineAtEnd())
        }

        override fun start() {
            sender.incrementReferenceCount()
        }

        override fun stop() {
            sender.decrementReferenceCount()
        }
    }

    private class ShellDestination(shellCommand: String, private val template: TemplateFormatter) : Destination {
        private val sender = ShellSender.createSender(shellCommand)

        override fun write(message: Message) {
            sender.writeMessagePayload(template.formatMessage(message).withNewlineAtEnd())
        }

        override fun stop() {
            sender.stop()
        }
    }
}
