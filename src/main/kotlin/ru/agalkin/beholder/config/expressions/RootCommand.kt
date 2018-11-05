package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.commands.BufferCommand
import ru.agalkin.beholder.commands.ConveyorCommandAbstract
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.conveyor.ConveyorInput

class RootCommand(app: Beholder) : ConveyorCommandAbstract(app, RootArguments) {
    lateinit var topLevelInput: ConveyorInput
    lateinit var topLevelOutput: Conveyor

    override fun createSubcommand(args: Arguments): CommandAbstract? {
        if (args.getCommandName() == "buffer") {
            if (subcommands.any { it is BufferCommand }) {
                throw CommandException("Command `buffer` cannot be duplicated")
            }
            return BufferCommand(app, args)
        }
        if (args.getCommandName() in ConfigOption.values().map { it.name.toLowerCase() }) {
            return ConfigOptionCommand(app, args)
        }
        return super.createSubcommand(args)
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        topLevelInput = conveyor.addInput("top level input in root")
        var currentConveyor = conveyor
        for (subcommand in subcommands) {
            currentConveyor = subcommand.buildConveyor(currentConveyor)
        }
        topLevelOutput = currentConveyor
        return currentConveyor
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

        override fun buildConveyor(conveyor: Conveyor): Conveyor {
            // Тут сообщения не ходят.
            // Если вдруг сообщение сюда попало, оно просто проходит насквозь без изменений.
            return conveyor
        }
    }
}
