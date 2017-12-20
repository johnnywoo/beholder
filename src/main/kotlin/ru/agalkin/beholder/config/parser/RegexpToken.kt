package ru.agalkin.beholder.config.parser

import ru.agalkin.beholder.charListToString
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class RegexpToken(private val delimiter: Char) : Token(initialChar = delimiter), ArgumentToken {
    private val body      = ArrayList<Char>()
    private var modifiers = 0

    // мы рассчитываем, что это свойство будет использовано
    // гарантированно после окончания парсинга токена
    // и парсинг токена происходит ровно один раз
    val regexp by lazy {
        try {
            Pattern.compile(charListToString(body), modifiers)!!
        } catch (e: PatternSyntaxException) {
            throw ParseException("Invalid regexp: ${e.message}")
        }
    }

    override fun getValue()
        = getDefinition()

    private var isInModifiers = false

    fun isSecondDelimiterPresent()
        = isInModifiers

    override fun addChar(char: Char): Token {
        when {
            isInModifiers -> {
                modifiers = modifiers or when (char) {
                    'i' -> Pattern.CASE_INSENSITIVE
                    'd' -> Pattern.UNIX_LINES
                    'x' -> Pattern.COMMENTS
                    'm' -> Pattern.MULTILINE
                    's' -> Pattern.DOTALL
                    'u' -> Pattern.UNICODE_CASE
                    'U' -> Pattern.UNICODE_CHARACTER_CLASS
                    in 'a'..'z', in 'A'..'Z', in '0'..'9' -> throw ParseException("Invalid regexp modifier: $char")
                    // не модификатор = это начинается новый токен
                    else -> return super.addChar(char)
                }
            }
            char == delimiter -> {
                isInModifiers = true
            }
            else -> {
                body.add(char)
            }
        }

        characters.add(char)
        return this
    }
}
