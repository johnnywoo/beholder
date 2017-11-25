package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.listeners.UdpListener

class FromCommand(arguments: List<ArgumentToken>) : CommandAbstract(arguments) {
    override fun createSubcommand(args: List<ArgumentToken>): CommandAbstract?
        = when (args[0].getValue()) {
            "parse" -> ParseCommand(args)
            else -> null
        }

    private val source: Source

    init {
        val usage = """
            |Usage:
            |from <source>
            |
            |Subcommands:
            |parse  -- populates message tags from logs in certain formats
            |
            |Sources:
            |udp [address:]port  -- receives messages from an UDP port
        """.trimMargin()

        try {
            source = when (requireArg(1, usage)) {
                "udp" -> {
                    requireNoArgsAfter(2)
                    UdpSource(Address.fromString(requireArg(2, usage), "0.0.0.0"))
                }
                else  -> throw CommandException(usage)
            }
        } catch (e: Address.AddressException) {
            throw CommandException(e)
        }
    }

    private fun onMessageFromSource(message: Message) {
        if (!subcommands.isEmpty()) {
            // есть детишки = сообщение из нашего источника сначала прогоняется через них
            subcommands[0].emit(message)
        } else {
            // нет детишек = сообщение напрямую вылезает из команды from
            emit(message)
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
                command.addReceiver { nextCommand.emit(it) }
            } else {
                // последний ребенок направляется в наш эмиттер
                // (сообщения, вылезающие из него, будут вылезать из команды from)
                command.addReceiver { emit(it) }
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
            println("${this::class.simpleName} start: connecting to UDP listener at $address")
            UdpListener.getListener(address).addReceiver(receiver)
        }

        override fun stop() {
            println("${this::class.simpleName} stop: disconnecting from UDP listener at $address")
            UdpListener.getListener(address).removeReceiver(receiver)
        }
    }
}

