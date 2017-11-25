package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException

class Beholder(private val configFile: String?) {
    // тут не ловим никаких ошибок, чтобы при старте с кривым конфигом сразу упасть
    var config: Config = readConfig()
    init {
        config.start()
    }

    fun reload() {
        val newConfig: Config
        try {
            newConfig = readConfig()
        } catch (e: ParseException) {
            println("=== Error: invalid config ===")
            println(e.message)
            println("=== Config was not applied ===")
            return
        }

        notifyBefore()

        config.stop()
        newConfig.start()
        config = newConfig

        notifyAfter()
    }

    private fun readConfig(): Config {
        if (configFile != null) {
            return Config.fromFile(configFile)
        }

        return Config.defaultConfig()
    }

    companion object {
        val receivers = mutableSetOf<ReloadListener>()

        private fun notifyBefore() {
            for (receiver in receivers) {
                receiver.before()
            }
        }

        private fun notifyAfter() {
            for (receiver in receivers) {
                receiver.after()
            }
        }
    }

    interface ReloadListener {
        fun before()
        fun after()
    }
}
