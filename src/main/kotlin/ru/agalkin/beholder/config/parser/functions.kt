package ru.agalkin.beholder.config.parser

fun charListToString(list: List<Char>): String {
    val sb = StringBuilder(list.size)
    for (char in list) {
        sb.append(char)
    }
    return sb.toString()
}

fun <T> listToString(list: List<T>, convert: (T) -> String): String {
    val sb = StringBuilder()
    for (element in list) {
        sb.append(convert(element))
    }
    return sb.toString()
}
