package ru.agalkin.beholder

import java.util.*

object GCTimer {
    private var timer = Timer()

    private val reloadListener = object : Beholder.ReloadListener {
        override fun before(app: Beholder) {
            timer.cancel()
            timer = Timer()
        }

        override fun after(app: Beholder) {
            val intervalSeconds = app.config.getIntOption(ConfigOption.EXTRA_GC_INTERVAL_SECONDS)
            InternalLog.info("GCTimer interval is $intervalSeconds seconds")
            if (intervalSeconds <= 0) {
                return
            }
            timer.schedule(
                object : TimerTask() {
                    override fun run() {
                        Runtime.getRuntime().gc()
                    }
                },
                0,
                intervalSeconds * 1000L
            )
        }
    }

    fun start() {
        InternalLog.info("GCTimer start")
        Beholder.reloadListeners.add(reloadListener)
    }
}
