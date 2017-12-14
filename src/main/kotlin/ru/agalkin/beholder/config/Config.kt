package ru.agalkin.beholder.config

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.commands.RootCommand
import ru.agalkin.beholder.config.parser.Token
import java.io.File

class Config(configText: String) {
    companion object {
        val help = """
            |Config structure
            |
            |Config contains commands, which can have subcommands (and so on).
            |Commands can produce, consume and modify messages.
            |Messages are collections of arbitrary fields.
            |
            |Example:
            |  flow {  # this command has no args, only subcommands
            |      from udp 3820 {  # this command has both args and subcommands
            |          parse syslog;  # this command has only arguments
            |      }
            |      to stdout;
            |  }
            |
            |
            |Config syntax
            |
            |Command arguments can be expressed as literal strings, quoted strings and regexps.
            |
            |Quoted strings start with either `'` or `"`. There is no difference between the two.
            |Escaping is done with backslashes. Special characters: `\n`, `\r`, `\t`.
            |If any other character is prefixed with `\`, it is stripped of the backslash.
            |Examples:
            |`'this \' is a quote'` => this ' is a quote
            |`'this \" is also a quote'` => this " is also a quote
            |`'\z'` => z
            |`'\n'` => newline character
            |`'\\n'` => \n
            |
            |Regexps are recognized by a delimiter, which is one of `/~!`.
            |The delimiter currently cannot be escaped in the regexp.
            |Regexp must be in the form of `#body#modifiers`. Modifiers are optional.
            |Examples:
            |`/spaces are allowed/`
            |`~http://[a-z.]+~`
            |`/cat|dog/i`
            |
            |Literal string is a string of non-whitespace characters that is not a quoted string or regexp.
            |There is no escaping in literals.
            |Example: `127.0.0.1:1234`.
            |
            |Config commands
            |
            |flow   -- defines flow of messages between commands
            |from   -- produces messages from some source
            |set    -- puts values into message fields
            |parse  -- populates message fields according to some format
            |to     -- sends messages to destinations
            |""".trimMargin()

        fun fromFile(filename: String): Config {
            InternalLog.info("Reading config from $filename")
            return Config.fromStringWithLog(File(filename).readText())
        }

        fun fromStringWithLog(configText: String): Config {
            InternalLog.info("=== Config text ===\n$configText=== End config text ===")
            val config = Config(configText)
            InternalLog.info("=== Parsed config ===\n${config.getDefinition()}=== End parsed config ===")
            return config
        }
    }

    private val root: RootCommand

    fun getDefinition()
        = root.getChildrenDefinition()

    init {
        // читаем символы из строки и формируем токены
        val tokens = Token.getTokens(configText)

        // окей, токены получились, теперь надо сделать из них команды
        // команда = набор токенов до терминатора (потенциально с детишками)
        // вариант 1: command arg arg;
        // вариант 2: command arg { block }
        // штука сразу делает конкретные экземпляры команд, в которых уже есть бизнес-логика
        root = RootCommand.fromTokens(tokens)
    }

    fun start()
        = root.start()

    fun stop()
        = root.stop()
}

