package ru.agalkin.beholder.config.parser

class QuotedStringToken(private val quoteChar: Char) : Token(initialChar = quoteChar), ArgumentToken {
    private val stringValue = ArrayList<Char>()

    override fun getValue() = charListToString(stringValue)

    private var isEscapeSequence = false

    override fun addChar(char: Char): Token {
        characters.add(char)

        if (isEscapeSequence) {
            isEscapeSequence = false
            stringValue.add(when (char) {
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                else -> char
            })
            return this
        }

        if (char == '\\') {
            isEscapeSequence = true
            return this
        }

        if (char == quoteChar) {
            // закрыли кавычки
            return Token()
        }

        stringValue.add(char)
        return this
    }
}
