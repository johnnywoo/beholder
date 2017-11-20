package ru.agalkin.beholder.config

import ru.agalkin.beholder.config.commands.RootCommand
import ru.agalkin.beholder.config.parser.Token
import java.io.File
import java.io.InputStreamReader

class Config(configText: String) {
    private val root: RootCommand

    init {
        println("=== Config text ===")
        print(configText)
        println("=== End config text ===")

        // читаем символы из строки и формируем токены
        val tokens = Token.getTokens(configText)

        // окей, токены получились, теперь надо сделать из них команды
        // команда = набор токенов до терминатора (потенциально с детишками)
        // вариант 1: command arg arg;
        // вариант 2: command arg { block }
        // штука сразу делает конкретные экземпляры команд, в которых уже есть бизнес-логика
        root = RootCommand.fromTokens(tokens)

        println("=== Parsed config ===")
        print(root.getChildrenDefinition())
        println("=== End parsed config ===")
    }

    fun start()
        = root.start()

    fun stop()
        = root.stop()

    companion object {
        fun fromFile(filename: String): Config {
            println("Reading config from $filename")
            return Config(File(filename).readText())
        }

        fun defaultConfig(): Config {
            println("Reading bundled config from jar resources")
            val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.conf")
            val configText  = InputStreamReader(inputStream).readText()
            return Config(configText)
        }
    }
}

