package ru.agalkin.beholder.config.commands

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



    private interface Source {
        fun start()
    }

    private inner class UdpSource(private val address: Address) : Source {
        override fun start() {
            println("from start: connecting to UDP listener at $address")
            UdpListener.getListener(address).addReceiver { emit(it) }
        }
    }
}

