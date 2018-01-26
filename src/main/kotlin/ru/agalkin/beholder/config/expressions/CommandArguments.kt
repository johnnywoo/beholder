package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.config.parser.LiteralToken

class CommandArguments(commandNameToken: LiteralToken) : Arguments() {
    init {
        addToken(commandNameToken)
    }

    override fun getCommandName()
        = args[0].getValue()
}
