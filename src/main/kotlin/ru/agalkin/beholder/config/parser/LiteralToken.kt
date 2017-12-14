package ru.agalkin.beholder.config.parser

class LiteralToken(initialChar: Char) : Token(initialChar), ArgumentToken {
    override fun getValue()
        = getDefinition()
}
