package ru.agalkin.beholder

import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

fun getIsoDateFormatter()
    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

// 2017-11-26T16:16:01+03:00
// 2017-11-26T16:16:01Z if UTC
fun getIsoDate(date: Date = Date()): String
    = getIsoDateFormatter().format(date)

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

fun addNewlineIfNeeded(text: String)
    = when (text.isEmpty() || text.last() != '\n') {
        true  -> text + "\n"
        false -> text
    }

fun <T : Comparable<T>> getClosestFromRange(range: ClosedRange<T>, item: T): T {
    if (item < range.start) {
        return range.start
    }
    if (item > range.endInclusive) {
        return range.endInclusive
    }
    return item
}
