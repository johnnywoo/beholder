package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.formatters.TemplateFormatter

class ToCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
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

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        conveyor.addStep(destination)
        return conveyor
    }

    override fun start() {
        destination.start()
        super.start()
    }

    override fun stop() {
        destination.stop()
        super.stop()
    }


    private abstract inner class Destination : Conveyor.Step {
        open fun start() {}
        open fun stop() {}

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    private inner class StdoutDestination(private val template: TemplateFormatter) : Destination() {
        override fun execute(message: Message): Conveyor.StepResult {
            print(template.formatMessage(message).withNewlineAtEnd().toString())
            return Conveyor.StepResult.CONTINUE
        }
    }

    private inner class FileDestination(
        private val filenameTemplate: TemplateFormatter,
        private val dataTemplate: TemplateFormatter
    ) : Destination() {

        override fun execute(message: Message): Conveyor.StepResult {
            val sender = app.fileSenders.getSender(filenameTemplate.formatMessage(message).toString())
            sender.writeMessagePayload(dataTemplate.formatMessage(message).withNewlineAtEnd())
            return Conveyor.StepResult.CONTINUE
        }
    }

    private inner class UdpDestination(address: Address, private val template: TemplateFormatter) : Destination() {
        private val sender = app.udpSenders.getSender(address)

        override fun execute(message: Message): Conveyor.StepResult {
            sender.writeMessagePayload(template.formatMessage(message))
            return Conveyor.StepResult.CONTINUE
        }
    }

    private inner class TcpDestination(address: Address, private val template: TemplateFormatter) : Destination() {
        private val sender = app.tcpSenders.getSender(address)

        override fun execute(message: Message): Conveyor.StepResult {
            sender.writeMessagePayload(template.formatMessage(message).withNewlineAtEnd())
            return Conveyor.StepResult.CONTINUE
        }

        override fun start() {
            sender.incrementReferenceCount()
        }

        override fun stop() {
            sender.decrementReferenceCount()
        }
    }

    private inner class ShellDestination(shellCommand: String, private val template: TemplateFormatter) : Destination() {
        private val sender = app.shellSenders.createSender(shellCommand)

        override fun execute(message: Message): Conveyor.StepResult {
            sender.writeMessagePayload(template.formatMessage(message).withNewlineAtEnd())
            return Conveyor.StepResult.CONTINUE
        }
    }
}
