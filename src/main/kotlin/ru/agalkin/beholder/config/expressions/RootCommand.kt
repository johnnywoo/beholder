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
                    val definition = arguments.shiftFixedString("An integer option value is required")
                    app.optionValues[option] = ConfigOption.intFromString(definition)
                }
                ConfigOption.Type.COMPRESSION -> {
                    val definition = arguments.shiftFixedString("Compression mode name is required")
                    app.optionValues[option] = ConfigOption.compressionFromString(definition)
                }
                ConfigOption.Type.TIMEZONE -> {
                    val definition = arguments.shiftFixedString("Timezone name is required")
                    app.optionValues[option] = ConfigOption.timezoneFromString(definition)
                }
            }
            arguments.end()
        }

        override fun input(message: Message) {
            // Ничего не делаем, тут сообщения не ходят
        }
    }
}
