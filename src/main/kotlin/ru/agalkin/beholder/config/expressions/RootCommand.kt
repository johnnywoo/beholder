package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.commands.ConveyorCommandAbstract
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token

class RootCommand(app: Beholder) : ConveyorCommandAbstract(
    app,
    RootArguments,
    sendInputToOutput = false,
    sendInputToSubcommands = false,
    sendLastSubcommandToOutput = false
) {
    val optionValues = hashMapOf<ConfigOption, Any>()
    init {
        for (option in ConfigOption.values()) {
            optionValues[option] = option.defaultValue
        }
    }

    override fun createSubcommand(args: Arguments): CommandAbstract? {
        if (args.getCommandName() in ConfigOption.values().map { it.name.toLowerCase() }) {
            return ConfigOptionCommand(app, args)
        }
        return super.createSubcommand(args)
    }

    companion object {
        fun fromTokens(app: Beholder, tokens: List<Token>): RootCommand {
            val root = RootCommand(app)
            val tokenIterator = tokens.listIterator()
            root.importSubcommands(tokenIterator)
            if (tokenIterator.hasNext()) {
                // не все токены распихались по выражениям
                throw ParseException.fromIterator("Unexpected leftover tokens:", tokenIterator)
            }
            return root
        }
    }

    private inner class ConfigOptionCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
        init {
            val option = ConfigOption.valueOf(arguments.getCommandName().toUpperCase())
            when (option.type) {
                ConfigOption.Type.INT -> {
                    val numberDefinition = arguments.shiftFixedString("An integer option value is required")
                    val match = "^(\\d+)([kmg])?$".toRegex(RegexOption.IGNORE_CASE).matchEntire(numberDefinition)
                    val n = match?.groups?.get(1)?.value?.toIntOrNull()
                    if (n == null) {
                        throw CommandException("An integer option value is required")
                    }
                    optionValues[option] = n * when (match.groups[2]?.value?.toLowerCase()) {
                        "g" -> 1024 * 1024 * 1024
                        "m" -> 1024 * 1024
                        "k" -> 1024
                        else -> 1
                    }
                }
            }
            arguments.end()
        }

        override fun input(message: Message) {
            // Ничего не делаем, тут сообщения не ходят
        }
    }
}
