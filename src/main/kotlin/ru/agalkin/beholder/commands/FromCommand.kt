package ru.agalkin.beholder.commands

import ru.agalkin.beholder.INTERNAL_LOG_FROM_FIELD
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.*
import ru.agalkin.beholder.threads.InternalLogListener
import ru.agalkin.beholder.threads.TIMER_FROM_FIELD
import ru.agalkin.beholder.threads.TimerListener
import ru.agalkin.beholder.threads.UdpListener

class FromCommand(arguments: Arguments) : CommandAbstract(arguments) {
    companion object {
        val help = """
            |from udp [<address>:]<port>;
            |from timer [<n> seconds];
            |from internal-log;
            |
            |Subcommands: `parse`, `set`.
            |
            |This command produces messages, applying subcommands to them if there are any.
            |
            |If there are any incoming messages (not produced by current `from` command),
            |`from` will copy them to its output. Subcommands are not applied to those.
            |
            |You can use subcommands to pre-process messages before placing them into the flow.
            |This way, you can easily receive messages in different formats from different sources.
            |
            |Example:
            |  flow {
            |      from udp 1001;
            |      from udp 1002 {parse syslog}
            |      set ¥payload dump;
            |      to stdout;
            |      # in stdout we will see raw messages from port 1001
            |      # and processed syslog messages from port 1002
            |  }
            |
            |Fields produced by `from udp`:
            |  ¥receivedDate  -- ISO date when the packet was received (example: 2017-11-26T16:22:31+03:00)
            |  ¥from          -- URI of packet source (example: udp://1.2.3.4:57733)
            |  ¥payload       -- Text as received from UDP
            |
            |`from timer` emits a minimal message every second.
            |It is useful for experimenting with beholder configurations.
            |You can specify a number of seconds between messages like this:
            |`from timer 30 seconds` for two messages per minute
            |`from timer 1 second` is the default and equivalent to just `from timer`.
            |
            |Fields produced by `from timer`:
            |  ¥receivedDate  -- ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
            |  ¥from          -- '$TIMER_FROM_FIELD'
            |  ¥syslogProgram -- 'beholder'
            |  ¥payload        -- A short random message
            |
            |`from internal-log` emits messages from the internal Beholder log. These are the same messages
            |Beholder writes to stdout/stderr and its log file (see also CLI options --log and --quiet).
            |
            |Fields produced by `from internal-log`:
            |  ¥receivedDate   -- ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
            |  ¥from           -- '$INTERNAL_LOG_FROM_FIELD'
            |  ¥syslogSeverity -- Severity of messages
            |  ¥syslogProgram  -- 'beholder'
            |  ¥payload        -- Log message text
            |""".trimMargin().replace("¥", "$")
    }

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
