package ru.agalkin.beholder

import java.io.InputStreamReader

fun readTextFromResource(name: String): String {
    val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(name)
    return InputStreamReader(inputStream).readText()
}

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
