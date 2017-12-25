package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.RegexpToken
import java.util.regex.Pattern

abstract class Arguments {
    abstract fun getCommandName(): String

    abstract fun toList(): List<ArgumentToken>

    abstract protected fun shiftToken(errorMessage: String): ArgumentToken

    abstract protected fun peekNext(skip: Int = 0): ArgumentToken?

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

    fun shiftSuffixedIntOrNull(suffixWords: Set<String>, errorMessage: String): Int? {
        // second token must be a literal word
        // if it's not, we just return null
        val suffixToken = peekNext(1)
        if (suffixToken == null || suffixToken !is LiteralToken || !suffixWords.contains(suffixToken.getValue())) {
            return null
        }

        // ok, there is the suffix word, so the first argument must be correct
        val token = shiftToken(errorMessage)

        // we need to move the index over to "consume" the suffix token
        shiftToken(errorMessage)

        if (token !is LiteralToken) {
            throw CommandException(errorMessage)
        }

        val number = token.getValue().toIntOrNull()
        if (number == null) {
            throw CommandException(errorMessage)
        }
        return number
    }

    fun shiftRegexp(errorMessage: String)
        = shiftRegexpOrNull() ?: throw CommandException(errorMessage)

    fun shiftRegexpOrNull(): Pattern? {
        val token = peekNext()
        if (token != null && token is RegexpToken) {
            shiftToken("")
            return token.regexp
        }
        return null
    }
}
