package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.MessageQueue
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.stats.Stats
import java.io.InterruptedIOException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

class TcpListenerThread(
    private val isListenerDeleted: AtomicBoolean,
    private val isSyslogFrame: Boolean,
    private val queue: MessageQueue,
    address: Address
) : Thread("from-tcp-$address-listener") {

    private val socket = ServerSocket(address.port, 2000, address.getInetAddress())
    init {
        socket.soTimeout = 50 // millis
    }

    override fun run() {
        while (!isListenerDeleted.get()) {
            try {
                val connection = socket.accept()
                if (isSyslogFrame) {
                    SyslogFrameTcpConnectionThread(connection, queue).start()
                } else {
                    NewlineTerminatedTcpConnectionThread(connection, queue).start()
                }
                Stats.reportTcpConnected()
            } catch (ignored: InterruptedIOException) {
                // ждём кусками по 50 мс, чтобы проверять isListenerDeleted
            }
        }
    }
}
