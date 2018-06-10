package ru.agalkin.beholder

import ru.agalkin.beholder.config.ConfigOption
import java.util.*

class GCTimer(private val app: Beholder) {
    private var timer = Timer()

    private val afterReload = {
        val intervalSeconds = app.config.getIntOption(ConfigOption.EXTRA_GC_INTERVAL_SECONDS)
        InternalLog.info("GCTimer interval is $intervalSeconds seconds")
        if (intervalSeconds > 0) {
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

        app.beforeReloadCallbacks.add {
            timer.cancel()
            timer = Timer()
        }

        app.afterReloadCallbacks.add(afterReload)
    }

    fun destroy() {
        timer.cancel()
        app.afterReloadCallbacks.remove(afterReload)
    }
}
