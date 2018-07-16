package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract

class FromCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    private val source: Source

    init {
        try {
            source = when (arguments.shiftAnyLiteral("`from` needs a type of message source")) {
                "udp" -> UdpSource(Address.fromString(
                    arguments.shiftFixedString("`from udp` needs at least a port number"),
                    "0.0.0.0"
                ))
                "tcp" -> {
                    val address = Address.fromString(
                        arguments.shiftFixedString("`from tcp` needs at least a port number"),
                        "0.0.0.0"
                    )

                    val isSyslogFrame: Boolean
                    if (arguments.shiftLiteralOrNull("as") != null) {
                        arguments.shiftLiteral("syslog-frame", "Correct syntax is `from tcp ... as syslog-frame`")
                        isSyslogFrame = true
                    } else {
                        isSyslogFrame = false
                    }

                    TcpSource(address, isSyslogFrame)
                }
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

    override fun start()
        = source.start()

    override fun stop()
        = source.stop()

    override fun input(message: Message) {
        output.sendMessageToSubscribers(message)
    }

    private interface Source {
        fun start()
        fun stop()
    }

    private inner class UdpSource(private val address: Address) : Source {
        private val receiver: (Message) -> Unit = { input(it) }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to UDP listener at $address")
            app.udpListeners.getListener(address).router.addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from UDP listener at $address")
            app.udpListeners.getListener(address).router.removeSubscriber(receiver)
        }
    }

    private inner class TcpSource(private val address: Address, isSyslogFrame: Boolean) : Source {
        private val receiver: (Message) -> Unit = { input(it) }
        init {
            if (!app.tcpListeners.setListenerMode(address, isSyslogFrame)) {
                throw CommandException("TCP listener for $address cannot be both newline-terminated and syslog-frame")
            }
        }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to TCP listener at $address")
            app.tcpListeners.getListener(address).router.addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from TCP listener at $address")
            app.tcpListeners.getListener(address).router.removeSubscriber(receiver)
        }
    }

    private inner class TimerSource(intervalSeconds: Int) : Source {
        private var secondsToSkip = 0
        private val receiver: (Message) -> Unit = {
            if (secondsToSkip <= 0) {
                secondsToSkip = intervalSeconds
                input(it)
            }
            secondsToSkip--
        }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to timer")
            app.timerListener.messageRouter.addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from timer")
            app.timerListener.messageRouter.removeSubscriber(receiver)
        }
    }

    private inner class InternalLogSource : Source {
        private val receiver: (Message) -> Unit = { input(it) }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to internal log")
            app.internalLogListener.router.addSubscriber(receiver)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from internal log")
            app.internalLogListener.router.removeSubscriber(receiver)
        }
    }
}

