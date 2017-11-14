package ru.agalkin.beholder.config.parser

class CommentToken : Token() {
    override fun addChar(char: Char): Token {
        if (char == '\n') {
            // строка кончилась = вылезаем из комментария
            return Token()
        }
        // игнорируем всё, что внутри комментария (не добавляем в characters)
        return this
    }
}
