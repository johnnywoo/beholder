package ru.agalkin.beholder.config.parser

import ru.agalkin.beholder.charListToString

open class Token(
    initialChar: Char? = null,
    private val isSingleChar: Boolean = false
) {
    // символы, составляющие определение токена
    // (как он был написан в конфиге)
    val characters = ArrayList<Char>()

    init {
        if (initialChar != null) {
            characters.add(initialChar)
        }
    }

    fun getDefinition()
        = charListToString(characters)

    private fun isEmpty()
        = characters.isEmpty()

    open protected fun addChar(char: Char): Token {
        // не внутри кавычек = может происходить смена токена
        when (char) {
            // пробелы = наш токен кончился, выдаём новый, сам пробел никуда не сохраняем
            ' ', '\t', '\n', '\r' -> return Token()
            // спецсимволы
            '{' -> return OpenBraceToken()
            '}' -> return CloseBraceToken()
            ';' -> return SemicolonToken()
            // токен комментария ничего не содержит, просто ждёт \n в пустом виде
            '#' -> return CommentToken()
            // delimiter = начинаем регулярку
            '~' -> return RegexpToken(char)
            // кавычки = начинаем новый кавычечный токен
            '"', '\'' -> return QuotedStringToken(char)
            // ничего особенного не нашли
            else -> {
                // произошла смена однобуквенного токена на другой
                // либо у нас в руках анонимный токен
                if (isSingleChar || this !is LiteralToken) {
                    return LiteralToken(char)
                }

                // просто нащупали новый символ к текущему токену
                characters.add(char)
                return this
            }
        }
    }

    companion object {
        fun getTokens(text: String): List<Token> {
            val tokens = ArrayList<Token>()
            var lastToken = Token()

            for (char in text) {
                val newToken = lastToken.addChar(char)
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
                throw ParseException("Unclosed string literal detected: ${lastToken.getDefinition()}")
            }

            if (lastToken is RegexpToken && !lastToken.isSecondDelimiterPresent()) {
                throw ParseException("Unclosed regexp detected: ${lastToken.getDefinition()}")
            }

            if (!lastToken.isEmpty()) {
                tokens.add(lastToken)
            }

            return tokens
        }
    }
}
