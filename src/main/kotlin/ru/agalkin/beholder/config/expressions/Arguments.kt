package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.RegexpToken
import java.util.regex.Pattern

abstract class Arguments {
    abstract fun getCommandName(): String

    abstract fun toList(): List<ArgumentToken>

    protected abstract fun shiftToken(errorMessage: String): ArgumentToken

    protected abstract fun peekNext(skip: Int = 0): ArgumentToken?

    abstract fun end()

    fun hasMoreTokens()
        = peekNext() != null

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

    fun shiftLiteralOrNull(words: Set<String>): String? {
        val token = peekNext()
        if (token is LiteralToken && token.getValue() in words) {
            shiftToken("")
            return token.getValue()
        }
        return null
    }

    fun shiftSuffixedIntOrNull(suffixWords: Set<String>, errorMessage: String): Int? {
        // second token must be a literal word
        // if it's not, we just return null
        val suffixToken = peekNext(1)
        if (suffixToken == null || suffixToken !is LiteralToken || suffixToken.getValue() !in suffixWords) {
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

    fun shiftPrefixedStringOrNull(prefixWords: Set<String>, errorMessage: String): String? {
        // first token must be a literal word
        // if it's not, we just return null
        val prefixToken = peekNext()
        if (prefixToken == null || prefixToken !is LiteralToken || prefixToken.getValue() !in prefixWords) {
            return null
        }

        // we need to move the index over to "consume" the prefix token
        shiftToken(errorMessage)

        // ok, there is the suffix word, so the second argument is required to be correct
        return shiftString(errorMessage)
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
