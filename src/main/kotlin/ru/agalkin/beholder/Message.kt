package ru.agalkin.beholder

import java.text.SimpleDateFormat
import java.util.*

data class Message(
    private val tags: MutableMap<String, String> = mutableMapOf<String, String>()
) {
    operator fun set(tag: String, value: String) {
        if (value.isEmpty()) {
            tags.remove(tag)
        } else {
            tags[tag] = value
        }
    }

    fun getTags(): Map<String, String>
        = tags

    fun getPayload(): String
        = stringTag("payload") ?: ""

    fun stringTag(tag: String): String? {
        val string = tags[tag]
        if (string == null || string.isEmpty()) {
            return null
        }
        return string
    }

    private var dateFormat: SimpleDateFormat? = null

    fun dateTag(tag: String): Date? {
        if (dateFormat == null) {
            dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        }
        val string = tags[tag]
        if (string == null || string.isEmpty()) {
            return null
        }
        return dateFormat!!.parse(string)
    }

    // default is a parameter to avoid boxing
    fun intTag(tag: String, default: Int): Int {
        try {
            val string = tags[tag]
            if (string == null || string.isEmpty()) {
                return default
            }
            return string.toInt()
        } catch (e: Throwable) {
            return default
        }
    }
}
