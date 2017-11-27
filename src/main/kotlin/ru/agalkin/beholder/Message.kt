package ru.agalkin.beholder

import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    private val fields: MutableMap<String, String> = mutableMapOf()
) {
    val messageId = createdMessagesCount++

    operator fun set(field: String, value: String) {
        if (value.isEmpty()) {
            fields.remove(field)
        } else {
            fields[field] = value
        }
    }

    fun getFields(): Map<String, String>
        = fields

    fun getPayload(): String
        = stringField("payload") ?: ""

    fun stringField(field: String): String? {
        val string = fields[field]
        if (string == null || string.isEmpty()) {
            return null
        }
        return string
    }

    private var dateFormat: SimpleDateFormat? = null

    fun dateField(field: String): Date? {
        if (dateFormat == null) {
            dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        }
        val string = fields[field]
        if (string == null || string.isEmpty()) {
            return null
        }
        return dateFormat!!.parse(string)
    }

    // default is a parameter to avoid boxing
    fun intField(field: String, default: Int): Int {
        try {
            val string = fields[field]
            if (string == null || string.isEmpty()) {
                return default
            }
            return string.toInt()
        } catch (e: Throwable) {
            return default
        }
    }

    companion object {
        var createdMessagesCount = 0L
    }
}
