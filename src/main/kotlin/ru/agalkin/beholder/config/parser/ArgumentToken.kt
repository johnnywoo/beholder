package ru.agalkin.beholder.config.parser

interface ArgumentToken {
    fun getValue(): String
    fun getDefinition(): String
}
