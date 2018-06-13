package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.listeners.InternalLogListener
import ru.agalkin.beholder.listeners.SelectorThread
import ru.agalkin.beholder.listeners.TcpListener
import ru.agalkin.beholder.listeners.UdpListener
import ru.agalkin.beholder.queue.DataBuffer
import ru.agalkin.beholder.senders.FileSender
import ru.agalkin.beholder.senders.ShellSender
import ru.agalkin.beholder.senders.TcpSender
import ru.agalkin.beholder.senders.UdpSender
import ru.agalkin.beholder.stats.Stats
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

const val BEHOLDER_SYSLOG_PROGRAM = "beholder"

class Beholder(private val configMaker: (Beholder) -> Config) : Closeable {
    val optionValues = ConcurrentHashMap<ConfigOption, Any>()
    init {
        for (option in ConfigOption.values()) {
            optionValues[option] = option.defaultValue
        }
    }

    val beforeReloadCallbacks = CopyOnWriteArraySet<() -> Unit>()
    val afterReloadCallbacks  = CopyOnWriteArraySet<() -> Unit>()

    val executor by lazy { Executor() }

    val defaultBuffer by lazy { DataBuffer(this) }

    val selectorThread by lazy {
        val st = SelectorThread(this)
        st.start()
        st
    }
    val tcpListeners by lazy { TcpListener.Factory(this) }
    val udpListeners by lazy { UdpListener.Factory(this) }
    val internalLogListener by lazy { InternalLogListener(this) }

    val fileSenders  by lazy { FileSender.Factory(this) }
    val shellSenders by lazy { ShellSender.Factory(this) }
    val tcpSenders   by lazy { TcpSender.Factory(this) }
    val udpSenders   by lazy { UdpSender.Factory(this) }

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

        notifyAfterReload()
    }

    /**
     * This method should only be used for tests!
     * Actual production Beholder app never closes.
     */
    override fun close() {
        notifyBeforeReload()

        config.stop()

        selectorThread.erase()
        internalLogListener.destroy()
        gcTimer.destroy()

        var needsWaiting = 0

        needsWaiting += tcpListeners.destroyAllListeners()
        needsWaiting += udpListeners.destroyAllListeners()

        executor.destroy()

        if (needsWaiting > 0) {
            // Give all blocking threads time to stop.
            // They should not block for more than 100 ms at a time.
            Thread.sleep(200)
        }
    }

    fun reload() {
        val newConfig: Config
        try {
            newConfig = configMaker(this)
        } catch (e: BeholderException) {
            InternalLog.err("=== Error: invalid config ===\n${e.message}\n=== Config was not applied ===")
            return
        }

        notifyBeforeReload()

        config.stop()
        config = newConfig
        config.start()

        notifyAfterReload()

        Stats.reportReload()
    }

    private fun notifyBeforeReload() {
        for (receiver in beforeReloadCallbacks) {
            receiver()
        }
    }

    private fun notifyAfterReload() {
        for (receiver in afterReloadCallbacks) {
            receiver()
        }
    }

    fun getIntOption(name: ConfigOption)
        = optionValues[name] as Int
}
