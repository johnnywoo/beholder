package ru.agalkin.beholder.commands

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.threads.InternalLogListener
import ru.agalkin.beholder.threads.TimerListener
import ru.agalkin.beholder.threads.UdpListener

class FromCommand(arguments: Arguments) : CommandAbstract(arguments) {
    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "parse" -> ParseCommand(args)
            "set"   -> SetCommand(args)
            else    -> null
        }

    private val source: Source

    init {
        try {
            source = when (arguments.shiftString("`from` needs a type of message source")) {
                "udp" -> UdpSource(Address.fromString(
                    arguments.shiftString("`from udp` needs at least a port number"),
                    "0.0.0.0"
                ))
                "timer" -> TimerSource(
                    arguments.shiftSuffixedIntOrNull(
                        setOf("second", "seconds"),
                        "Correct syntax is `from timer 10 seconds`"
                    ) ?: 1
                )
                "internal-log" -> InternalLogSource()
                else -> throw CommandException("Cannot understand arguments of `from` command")
            }
        } catch (e: Address.AddressException) {
            throw CommandException(e).apply { addSuppressed(e) }
        }

        arguments.end()
    }

    private fun onMessageFromSource(message: Message) {
        if (!subcommands.isEmpty()) {
            // есть детишки = сообщение из нашего источника сначала прогоняется через них
            subcommands[0].receiveMessage(message)
        } else {
            // нет детишек = сообщение напрямую вылезает из команды from
            receiveMessage(message)
        }
    }

    override fun start() {
        // детишки соединяются в цепочку
        for (i in subcommands.indices) {
            val command = subcommands[i]
            if (subcommands.indices.contains(i + 1)) {
                val nextCommand = subcommands[i + 1]
                // не последний ребенок направляется в следующего
                // (сообщения, вылезающие из него, попадают в следующего ребенка)
                command.router.addSubscriber({ nextCommand.receiveMessage(it) })
            } else {
                // последний ребенок направляется в наш эмиттер
                // (сообщения, вылезающие из него, будут вылезать из команды from)
                command.router.addSubscriber({ receiveMessage(it) })
            }
        }

        // включаем лисенер
        source.start()
    }

    override fun stop()
        = source.stop()



    private interface Source {
        fun start()
        fun stop()
    }

    private inner class UdpSource(private val address: Address) : Source {
        val receiver: (Message) -> Unit = { onMessageFromSource(it) }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to UDP listener at $address")
            UdpListener.getListener(address).router.addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from UDP listener at $address")
            UdpListener.getListener(address).router.removeSubscriber(receiver)
        }
    }

    private inner class TimerSource(intervalSeconds: Int) : Source {
        private var secondsToSkip = 0
        val receiver: (Message) -> Unit = {
            if (secondsToSkip <= 0) {
                secondsToSkip = intervalSeconds
                onMessageFromSource(it)
            }
            secondsToSkip--
        }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to timer")
            TimerListener.messageRouter.addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from timer")
            TimerListener.messageRouter.removeSubscriber(receiver)
        }
    }

    private inner class InternalLogSource : Source {
        val receiver: (Message) -> Unit = { onMessageFromSource(it) }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to internal log")
            InternalLogListener.getMessageRouter().addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from internal log")
            InternalLogListener.getMessageRouter().removeSubscriber(receiver)
        }
    }
}

