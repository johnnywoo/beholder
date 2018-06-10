package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.listeners.InternalLogListener
import ru.agalkin.beholder.listeners.SelectorThread
import ru.agalkin.beholder.listeners.TcpListener
import ru.agalkin.beholder.listeners.UdpListener
import ru.agalkin.beholder.senders.FileSender
import ru.agalkin.beholder.senders.ShellSender
import ru.agalkin.beholder.senders.TcpSender
import ru.agalkin.beholder.senders.UdpSender
import ru.agalkin.beholder.stats.Stats
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

const val BEHOLDER_SYSLOG_PROGRAM = "beholder"

class Beholder(private val configMaker: (Beholder) -> Config) : Closeable {
    val beforeReloadCallbacks = CopyOnWriteArraySet<() -> Unit>()
    val afterReloadCallbacks = CopyOnWriteArraySet<() -> Unit>()

    val selectorThread by lazy {
        val st = SelectorThread()
        st.start()
        st
    }
    val tcpListeners by lazy { TcpListener.Factory(this) }
    val udpListeners by lazy { UdpListener.Factory(this) }
    val internalLogListener by lazy { InternalLogListener(this) }

    val fileSenders by lazy { FileSender.Factory(this) }
    val shellSenders by lazy { ShellSender.Factory(this) }
    val tcpSenders by lazy { TcpSender.Factory(this) }
    val udpSenders by lazy { UdpSender.Factory(this) }

    // we need very little memory compared to most Java programs
    // let's shrink the heap
    private val gcTimer = GCTimer(this)
    init {
        gcTimer.start()
    }

    // тут не ловим никаких ошибок, чтобы при старте с кривым конфигом сразу упасть
    var config: Config = configMaker(this)

    fun start() {
        config.start()

        notifyAfter()
    }

    override fun close() {
        notifyBefore()

        config.stop()

        selectorThread.erase()
        internalLogListener.destroy()

        var needsWaiting = 0

        needsWaiting += tcpListeners.destroyAllListeners()
        needsWaiting += udpListeners.destroyAllListeners()

        needsWaiting += fileSenders.destroyAllSenders()
        needsWaiting += shellSenders.destroyAllSenders()
        needsWaiting += tcpSenders.destroyAllSenders()
        needsWaiting += udpSenders.destroyAllSenders()

        if (needsWaiting > 0) {
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

        notifyBefore()

        config.stop()
        config = newConfig
        config.start()

        notifyAfter()

        Stats.reportReload()
    }

    private fun notifyBefore() {
        for (receiver in beforeReloadCallbacks) {
            receiver()
        }
    }

    private fun notifyAfter() {
        for (receiver in afterReloadCallbacks) {
            receiver()
        }
    }
}
