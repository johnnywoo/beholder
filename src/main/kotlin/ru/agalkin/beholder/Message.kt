package ru.agalkin.beholder

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val createdMessagesCount = AtomicLong(0)

class Message {
    private val fields: MutableMap<String, String> = mutableMapOf()

    val messageId = createdMessagesCount.getAndIncrement()

    fun copy(): Message {
        val newMessage = Message()
        newMessage.fields.putAll(fields)
        return newMessage
    }

    operator fun set(field: String, value: String) {
        if (value.isEmpty()) {
            fields.remove(field)
        } else {
            fields[field] = value
        }
    }

    operator fun get(field: String): String? {
        val value = fields[field]
        if (value == null || value.isEmpty()) {
            return null
        }
        return value
    }

    fun getFields(): Map<String, String>
        = fields

    fun getPayload(): String
        = get("payload") ?: ""

    private var dateFormat: SimpleDateFormat? = null

    fun dateField(field: String): Date? {
        if (dateFormat == null) {
            dateFormat = getIsoDateFormatter()
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
}
