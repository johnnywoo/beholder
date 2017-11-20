package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.listeners.UdpListener

class FromCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    private val source: Source

    init {
        val usage = "Usage:\n" +
            "from udp [host:]port"

        try {
            source = when (requireArg(1, usage)) {
                "udp" -> UdpSource(Address.fromString(requireArg(2, usage), "0.0.0.0"))
                else  -> throw CommandException(usage)
            }
        } catch (e: Address.AddressException) {
            throw CommandException(e)
        }
    }

    override fun start()
        = source.start()

    override fun stop()
        = source.stop()



    private interface Source {
        fun start()
        fun stop()
    }

    private inner class UdpSource(private val address: Address) : Source {
        val receiver: (Message) -> Unit = { emit(it) }

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

