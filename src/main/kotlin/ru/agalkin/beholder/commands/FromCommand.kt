package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.conveyor.ConveyorInput
import ru.agalkin.beholder.listeners.MockListener
import java.util.concurrent.atomic.AtomicLong

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
                "infinity" -> InfinitySource((arguments.shiftAnyLiteralOrNull() ?: "100").toInt())
                "internal-log" -> InternalLogSource()
                "mock" -> MockSource(arguments.shiftAnyLiteralOrNull() ?: "default")
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

    private lateinit var conveyorInput: ConveyorInput

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        conveyorInput = conveyor.addInput(getDefinition(includeSubcommands = false))
        return conveyor
    }

    private interface Source {
        fun start()
        fun stop()
    }

    private inner class UdpSource(private val address: Address) : Source {
        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to UDP listener at $address")
            app.udpListeners.getListener(address).router.addSubscriber(conveyorInput)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from UDP listener at $address")
            app.udpListeners.getListener(address).router.removeSubscriber(conveyorInput)
        }
    }

    private inner class TcpSource(private val address: Address, isSyslogFrame: Boolean) : Source {
        init {
            if (!app.tcpListeners.setListenerMode(address, isSyslogFrame)) {
                throw CommandException("TCP listener for $address cannot be both newline-terminated and syslog-frame")
            }
        }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to TCP listener at $address")
            app.tcpListeners.getListener(address).router.addSubscriber(conveyorInput)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from TCP listener at $address")
            app.tcpListeners.getListener(address).router.removeSubscriber(conveyorInput)
        }
    }

    private inner class TimerSource(intervalSeconds: Int) : Source {
        private val nextMillis = AtomicLong(System.currentTimeMillis())
        private val timerInput = object : ConveyorInput {
            override fun addMessage(message: Message) {
                if (nextMillis.get() <= System.currentTimeMillis()) {
                    nextMillis.addAndGet(intervalSeconds * 1000L)
                    conveyorInput.addMessage(message)
                }
            }
        }

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to timer")
            app.timerListener.messageRouter.addSubscriber(timerInput)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from timer")
            app.timerListener.messageRouter.removeSubscriber(timerInput)
        }
    }

    private inner class InfinitySource(private val messageLengthBytes: Int) : Source {
        private val listener by lazy {app.infinityListeners.addListener(messageLengthBytes)}

        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to infinity")
            listener.router.addSubscriber(conveyorInput)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from infinity")
            listener.router.removeSubscriber(conveyorInput)
        }
    }

    private inner class InternalLogSource : Source {
        override fun start() {
            InternalLog.info("${this::class.simpleName} start: connecting to internal log")
            app.internalLogListener.router.addSubscriber(conveyorInput)
        }

        override fun stop() {
            InternalLog.info("${this::class.simpleName} stop: disconnecting from internal log")
            app.internalLogListener.router.removeSubscriber(conveyorInput)
        }
    }

    private inner class MockSource(name: String) : Source {
        private val mockListener = MockListener(app)
        init {
            app.mockListeners[name] = mockListener
        }

        override fun start() {
            mockListener.router.addSubscriber(conveyorInput)
        }

        override fun stop() {
            mockListener.router.removeSubscriber(conveyorInput)
        }
    }
}

