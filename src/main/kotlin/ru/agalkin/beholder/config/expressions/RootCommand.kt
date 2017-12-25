package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.commands.FlowCommand
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token

class RootCommand : FlowCommand(RootArguments) {
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
