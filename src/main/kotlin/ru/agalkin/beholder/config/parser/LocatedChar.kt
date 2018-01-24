package ru.agalkin.beholder.config.parser

data class LocatedChar(
    val char: Char,
    private val source: String,
    private val line: Int
) {
    fun getLocationSummary()
        = "[$source:$line]"
}
