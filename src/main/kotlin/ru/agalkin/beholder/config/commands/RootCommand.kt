package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.*

class RootCommand : CommandAbstract(Arguments(arrayListOf())) {
    override fun createSubcommand(args: Arguments) : CommandAbstract?
        = when (args.getCommandName()) {
            "flow" -> FlowCommand(args)
            else   -> null
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
}
