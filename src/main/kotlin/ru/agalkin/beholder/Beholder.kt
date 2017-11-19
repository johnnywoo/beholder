package ru.agalkin.beholder

import ru.agalkin.beholder.config.Config

class Beholder(configFile: String?) {
    val config: Config = if (configFile != null) {
        Config.fromFile(configFile)
    } else {
        Config.defaultConfig()
    }

    fun start()
        = config.start()
}
