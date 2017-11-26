package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.*

class RootCommand : CommandAbstract(arrayListOf()) {
    override fun createSubcommand(args: List<ArgumentToken>) : CommandAbstract?
        = when (args[0].getValue()) {
            "flow" -> FlowCommand(args)
            else -> null
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
