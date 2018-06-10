package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.listeners.SelectorThread
import ru.agalkin.beholder.listeners.TcpListener
import ru.agalkin.beholder.listeners.UdpListener
import java.io.Closeable
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

const val BEHOLDER_SYSLOG_PROGRAM = "beholder"

class Beholder(private val configMaker: (Beholder) -> Config) : Closeable {
    val selectorThread = SelectorThread()
    val tcpListeners = TcpListener.Factory(this)
    val udpListeners = UdpListener.Factory(this)
    init {
        selectorThread.start()
    }

    // тут не ловим никаких ошибок, чтобы при старте с кривым конфигом сразу упасть
    var config: Config = configMaker(this)

    fun start() {
        config.start()
        uptimeDate = Date()

        notifyAfter(this)
    }

    override fun close() {
        notifyBefore(this)
        config.stop()
        selectorThread.erase()
        val tcpDestroyed = tcpListeners.destroyAllListeners()
        val udpDestroyed = udpListeners.destroyAllListeners()

        if (tcpDestroyed + udpDestroyed > 0) {
            // Give all blocking threads time to stop
            Thread.sleep(200)
        }
    }

    fun reload() {
        val newConfig: Config
        try {
            newConfig = configMaker(this)
        } catch (e: ParseException) {
            InternalLog.err("=== Error: invalid config ===")
            InternalLog.err(e.message)
            InternalLog.err("=== Config was not applied ===")
            return
        }

        notifyBefore(this)

        config.stop()
        config = newConfig
        config.start()

        notifyAfter(this)
    }

    companion object {
        val reloadListeners = CopyOnWriteArraySet<ReloadListener>()

        var uptimeDate: Date? = null

        private fun notifyBefore(app: Beholder) {
            for (receiver in reloadListeners) {
                receiver.before(app)
            }
        }

        private fun notifyAfter(app: Beholder) {
            for (receiver in reloadListeners) {
                receiver.after(app)
            }
        }
    }

    interface ReloadListener {
        fun before(app: Beholder)
        fun after(app: Beholder)
    }
}
