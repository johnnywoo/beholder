package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException

const val BEHOLDER_SYSLOG_PROGRAM = "beholder"

class Beholder(private val configFile: String?, private val configText: String?) {
    // тут не ловим никаких ошибок, чтобы при старте с кривым конфигом сразу упасть
    var config: Config = readConfig()

    fun start()
        = config.start()

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

        notifyBefore()

        config.stop()
        config = newConfig
        config.start()

        notifyAfter()
    }

    private fun readConfig(): Config {
        if (configText != null) {
            return Config(configText)
        }

        if (configFile != null) {
            return Config.fromFile(configFile)
        }

        throw BeholderException("Cannot start beholder without config")
    }

    companion object {
        val reloadListeners = mutableSetOf<ReloadListener>()

        private fun notifyBefore() {
            for (receiver in reloadListeners) {
                receiver.before()
            }
        }

        private fun notifyAfter() {
            for (receiver in reloadListeners) {
                receiver.after()
            }
        }
    }

    interface ReloadListener {
        fun before()
        fun after()
    }
}
