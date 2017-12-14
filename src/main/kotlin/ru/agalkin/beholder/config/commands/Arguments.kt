package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.RegexpToken
import java.util.regex.Pattern

interface Arguments {
    fun getCommandName(): String

    fun toList(): List<ArgumentToken>

    fun shiftToken(errorMessage: String): ArgumentToken

    fun shift(errorMessage: String)
        = shiftToken(errorMessage).getValue()

    fun shiftFieldName(errorMessage: String): String {
        val arg = shiftToken(errorMessage)
        if (arg !is LiteralToken || !arg.getValue().startsWith('$')) {
            throw CommandException(errorMessage)
        }
        return arg.getValue().substring(1)
    }

    fun shiftRegexp(errorMessage: String): Pattern {
        val arg = shiftToken(errorMessage)
        if (arg !is RegexpToken) {
            throw CommandException(errorMessage)
        }
        return arg.regexp
    }

    fun end()
}
