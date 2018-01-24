package ru.agalkin.beholder.config.parser

class LiteralToken(initialChar: LocatedChar) : Token(initialChar), ArgumentToken {
    override fun getValue()
        = getDefinition()
}
