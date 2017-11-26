package ru.agalkin.beholder.config

import ru.agalkin.beholder.BeholderException
import java.net.InetAddress

data class Address(private val host: String, val port: Int) {
    companion object {
        fun fromString(str: String, defaultHost: String): Address {
            val match = "^(?:([^:]+):)?([0-9]+)$".toRegex().matchEntire(str)?.groups
            if (match == null) {
                throw AddressException("Invalid network address: $str")
            }

            val matchedHost = match[1]?.value
            val matchedPort = match[2]?.value!!

            val host = matchedHost ?: defaultHost

            val port: Int
            try {
                port = matchedPort.toInt()
            } catch (e: Throwable) {
                throw AddressException("Invalid port: $matchedPort")
            }

            if (port < 1) {
                throw AddressException("Invalid port: $matchedPort")
            }

            return Address(host, port)
        }
    }

    override fun toString()
        = "$host:$port"

    fun getInetAddress(): InetAddress
        = InetAddress.getByName(host)

    class AddressException(message: String) : BeholderException(message)
}

