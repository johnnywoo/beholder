package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

const val BEHOLDER_SYSLOG_PROGRAM = "beholder"

class Beholder(private val configFile: String?, private val configText: String?, private val configSourceDescription: String?) {
    // тут не ловим никаких ошибок, чтобы при старте с кривым конфигом сразу упасть
    var config: Config = readConfig()

    fun start() {
        config.start()
        uptimeDate = Date()

        notifyAfter(this)
    }

    fun reload() {
        val newConfig: Config
        try {
            newConfig = readConfig()
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

    private fun readConfig(): Config {
        if (configText != null) {
            return Config.fromStringWithLog(configText, configSourceDescription ?: "unknown")
        }

        if (configFile != null) {
            return Config.fromFile(configFile)
        }

        throw BeholderException("Cannot start beholder without config")
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
