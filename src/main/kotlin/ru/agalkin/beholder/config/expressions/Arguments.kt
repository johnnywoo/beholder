package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.QuotedStringToken
import ru.agalkin.beholder.config.parser.RegexpToken
import ru.agalkin.beholder.formatters.TemplateFormatter
import java.util.regex.Pattern

abstract class Arguments {
    abstract fun getCommandName(): String

    protected val args = mutableListOf<ArgumentToken>()

    private var index = 0

    fun toList()
        = args.toList()

    private fun peekNext(skip: Int = 0)
        = args.getOrNull(index + 1 + skip)

    private fun shiftToken(errorMessage: String): ArgumentToken {
        val token = peekNext() ?: throw CommandException(errorMessage)
        index++
        return token
    }

    fun addToken(argumentToken: ArgumentToken)
        = args.add(argumentToken)

    fun hasMoreTokens()
        = peekNext() != null

    fun end() {
        if (hasMoreTokens()) {
            throw CommandException("Too many arguments for `${getCommandName()}`")
        }
    }

    private fun shiftString(errorMessage: String): String {
        val token = shiftToken(errorMessage)
        if (token !is LiteralToken && token !is QuotedStringToken) {
            throw CommandException(errorMessage)
        }
        return token.getValue()
    }

    // string that has no template variables
    fun shiftFixedString(errorMessage: String): String {
        val string = shiftString(errorMessage)
        if (TemplateFormatter.hasTemplates(string)) {
            throw CommandException(errorMessage + " (message fields are not allowed here)")
        }
        return string
    }

    fun shiftStringTemplate(errorMessage: String)
        = TemplateFormatter.create(shiftString(errorMessage))

    fun shiftFieldName(errorMessage: String): String {
        val arg = shiftAnyLiteral(errorMessage)
        if (!(arg matches Regex("^\\$([a-z][a-z0-9_]*)$", RegexOption.IGNORE_CASE))) {
            throw CommandException(errorMessage)
        }
        return arg.substring(1)
    }

    fun shiftAnyLiteral(errorMessage: String)
        = shiftAnyLiteralOrNull() ?: throw CommandException(errorMessage)

    fun shiftAnyLiteralOrNull(): String? {
        val token = peekNext()
        if (token is LiteralToken) {
            shiftToken("")
            return token.getValue()
        }
        return null
    }

    fun shiftLiteralOrNull(vararg words: String): String? {
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
        val arg = shiftAnyLiteral(errorMessage)

        // we need to move the index over to "consume" the suffix token
        shiftToken(errorMessage)

        return arg.toIntOrNull() ?: throw CommandException(errorMessage)
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
