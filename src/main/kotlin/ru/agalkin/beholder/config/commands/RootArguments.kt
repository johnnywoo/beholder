package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken

object RootArguments : Arguments() {
    override fun toList(): List<ArgumentToken>
        = listOf()

    override fun shiftToken(errorMessage: String): ArgumentToken {
        throw CommandException(errorMessage)
    }

    override fun peekNext(skip: Int)
        = null

    override fun end() {
    }

    override fun getCommandName()
        = "<root>"
}
