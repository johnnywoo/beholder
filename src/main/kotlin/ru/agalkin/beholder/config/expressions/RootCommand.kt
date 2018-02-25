package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.ConfigOption
import ru.agalkin.beholder.commands.FlowCommand
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token

class RootCommand : FlowCommand(RootArguments) {
    val optionValues = hashMapOf<ConfigOption, Any>()
    init {
        for (option in ConfigOption.values()) {
            optionValues[option] = option.defaultValue
        }
    }

    override fun createSubcommand(args: Arguments): CommandAbstract? {
        if (args.getCommandName() in ConfigOption.values().map { it.name.toLowerCase() }) {
            return ConfigOptionCommand(args)
        }
        return super.createSubcommand(args)
    }

    companion object {
        fun fromTokens(tokens: List<Token>): RootCommand {
            val root = RootCommand()
            val tokenIterator = tokens.listIterator()
            root.importSubcommands(tokenIterator)
            if (tokenIterator.hasNext()) {
                // не все токены распихались по выражениям
                throw ParseException.fromIterator("Unexpected leftover tokens:", tokenIterator)
            }
            return root
        }
    }

    private inner class ConfigOptionCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
        init {
            val option = ConfigOption.valueOf(arguments.getCommandName().toUpperCase())
            when (option.type) {
                ConfigOption.Type.INT -> {
                    optionValues[option] = arguments.shiftFixedString("An integer value is required").toInt()
                }
            }
            arguments.end()
        }
    }
}
