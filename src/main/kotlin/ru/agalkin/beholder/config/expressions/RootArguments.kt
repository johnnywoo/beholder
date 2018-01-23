package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.config.parser.ArgumentToken

object RootArguments : Arguments() {
    override fun toList(): List<ArgumentToken>
        = listOf()

    override fun shiftToken(errorMessage: String): ArgumentToken {
        throw CommandException(errorMessage)
    }

    override fun peekNext(skip: Int): ArgumentToken?
        = null

    override fun end() {}

    override fun getCommandName()
        = "<root>"
}
