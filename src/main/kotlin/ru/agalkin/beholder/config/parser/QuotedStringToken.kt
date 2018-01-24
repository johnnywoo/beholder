package ru.agalkin.beholder.config.parser

import ru.agalkin.beholder.charListToString

class QuotedStringToken(initialChar: LocatedChar) : Token(initialChar), ArgumentToken {
    private val quoteChar = initialChar.char

    private val stringValue = ArrayList<Char>()

    override fun getValue()
        = charListToString(stringValue)

    private var isEscapeSequence = false

    override fun addChar(locatedChar: LocatedChar): Token {
        characters.add(locatedChar)

        if (isEscapeSequence) {
            isEscapeSequence = false
            stringValue.add(when (locatedChar.char) {
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                else -> locatedChar.char
            })
            return this
        }

        if (locatedChar.char == '\\') {
            isEscapeSequence = true
            return this
        }

        if (locatedChar.char == quoteChar) {
            // закрыли кавычки
            return Token()
        }

        stringValue.add(locatedChar.char)
        return this
    }
}
