package ru.agalkin.beholder.config

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.expressions.RootCommand
import ru.agalkin.beholder.config.parser.Token
import ru.agalkin.beholder.conveyor.Conveyor
import java.io.File

class Config(app: Beholder, configText: String, configSourceDescription: String) {
    companion object {
        fun fromFile(app: Beholder, filename: String): Config {
            InternalLog.info("Reading config from $filename")
            return Config.fromStringWithLog(app, File(filename).readText(), filename)
        }

        fun fromStringWithLog(app: Beholder, configText: String, sourceDescription: String): Config {
            InternalLog.info("=== Config text ===\n$configText=== End config text ===")
            val config = Config(app, configText, sourceDescription)
            InternalLog.info("=== Parsed config ===\n${config.getDefinition()}=== End parsed config ===")
            return config
        }
    }

    val root: RootCommand
    val initialConveyor = Conveyor.createInitialConveyor()

    fun getDefinition()
        = root.getChildrenDefinition()

    init {
        // читаем символы из строки и формируем токены
        val tokens = Token.getTokens(configText, configSourceDescription)

        // окей, токены получились, теперь надо сделать из них команды
        // команда = набор токенов до терминатора (потенциально с детишками)
        // вариант 1: command arg arg;
        // вариант 2: command arg { block }
        // штука сразу делает конкретные экземпляры команд, в которых уже есть бизнес-логика
        root = RootCommand.fromTokens(app, tokens)

        root.buildConveyor(initialConveyor)
    }

    fun start()
        = root.start()

    fun stop()
        = root.stop()
}
