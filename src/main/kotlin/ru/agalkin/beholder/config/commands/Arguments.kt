package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.RegexpToken
import java.util.regex.Pattern

abstract class Arguments {
    abstract fun getCommandName(): String

    abstract fun toList(): List<ArgumentToken>

    abstract protected fun shiftToken(errorMessage: String): ArgumentToken

    abstract fun end()

    fun shiftStringToken(errorMessage: String): ArgumentToken {
        val token = shiftToken(errorMessage)
        if (token is RegexpToken) {
            throw CommandException(errorMessage)
        }
        return token
    }

    fun shiftString(errorMessage: String)
        = shiftStringToken(errorMessage).getValue()

    fun shiftFieldName(errorMessage: String): String {
        val token = shiftToken(errorMessage)
        if (token !is LiteralToken) {
            throw CommandException(errorMessage)
        }
        val arg = token.getValue()
        if (!(arg matches Regex("^\\$([a-z][a-z0-9_]*)$", RegexOption.IGNORE_CASE))) {
            throw CommandException(errorMessage)
        }
        return arg.substring(1)
    }

    fun shiftRegexp(errorMessage: String): Pattern {
        val arg = shiftToken(errorMessage)
        if (arg !is RegexpToken) {
            throw CommandException(errorMessage)
        }
        return arg.regexp
    }
}
