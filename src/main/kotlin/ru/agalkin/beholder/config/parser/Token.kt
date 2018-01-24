package ru.agalkin.beholder.config.parser

import ru.agalkin.beholder.substringUpTo

open class Token(
    initialChar: LocatedChar? = null,
    private val isSingleChar: Boolean = false
) {
    // символы, составляющие определение токена
    // (как он был написан в конфиге)
    protected val characters = ArrayList<LocatedChar>()

    init {
        if (initialChar != null) {
            characters.add(initialChar)
        }
    }

    fun getDefinition(): String {
        val sb = StringBuilder(characters.size)
        for (locatedChar in characters) {
            sb.append(locatedChar.char)
        }
        return sb.toString()
    }

    fun getLocationSummary()
        = characters.first().getLocationSummary()

    fun getDefinitionWithLocation(): String {
        val sb = StringBuilder()
        sb.append(substringUpTo(getDefinition(), 30))
        if (!characters.isEmpty()) {
            sb.append(' ').append(getLocationSummary())
        }
        return sb.toString()
    }

    private fun isEmpty()
        = characters.isEmpty()

    protected open fun addChar(locatedChar: LocatedChar): Token {
        // не внутри кавычек = может происходить смена токена
        when (locatedChar.char) {
            // пробелы = наш токен кончился, выдаём новый, сам пробел никуда не сохраняем
            ' ', '\t', '\n', '\r' -> return Token()
            // спецсимволы
            '{' -> return OpenBraceToken(locatedChar)
            '}' -> return CloseBraceToken(locatedChar)
            ';' -> return SemicolonToken(locatedChar)
            // токен комментария ничего не содержит, просто ждёт \n в пустом виде
            '#' -> return CommentToken()
            // delimiter = начинаем регулярку
            '~' -> return RegexpToken(locatedChar)
            // кавычки = начинаем новый кавычечный токен
            '"', '\'' -> return QuotedStringToken(locatedChar)
            // ничего особенного не нашли
            else -> {
                // произошла смена однобуквенного токена на другой
                // либо у нас в руках анонимный токен
                if (isSingleChar || this !is LiteralToken) {
                    return LiteralToken(locatedChar)
                }

                // просто нащупали новый символ к текущему токену
                characters.add(locatedChar)
                return this
            }
        }
    }

    companion object {
        fun getTokens(text: String, sourceDescription: String): List<Token> {
            val tokens = ArrayList<Token>()
            var lastToken = Token()

            var lineNumber = 1

            for (char in text) {
                val locatedChar = LocatedChar(char, sourceDescription, lineNumber)

                if (char == '\n') {
                    lineNumber++
                }

                val newToken = lastToken.addChar(locatedChar)
                if (newToken != lastToken) {
                    // начался новый токен
                    // добавляем предыдущий в список, если там не пусто
                    if (!lastToken.isEmpty()) {
                        tokens.add(lastToken)
                    }
                    lastToken = newToken
                }
            }

            if (lastToken is QuotedStringToken) {
                throw ParseException("Unclosed string literal detected: ${lastToken.getDefinitionWithLocation()}")
            }

            if (lastToken is RegexpToken && !lastToken.isSecondDelimiterPresent()) {
                throw ParseException("Unclosed regexp detected: ${lastToken.getDefinitionWithLocation()}")
            }

            if (!lastToken.isEmpty()) {
                tokens.add(lastToken)
            }

            return tokens
        }
    }
}
