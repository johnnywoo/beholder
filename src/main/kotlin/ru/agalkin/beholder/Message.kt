package ru.agalkin.beholder

import java.text.ParseException
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

    fun getFields(): Map<String, String>
        = fields

    fun getPayload()
        = getStringField("payload")

    fun getStringField(field: String, default: String = "")
        = fields[field] ?: default

    private var dateFormat: SimpleDateFormat? = null

    fun getDateField(field: String): Date? {
        var format = dateFormat
        if (format == null) {
            format = getIsoDateFormatter()
            dateFormat = format
        }
        val string = fields[field]
        if (string == null) {
            return null
        }
        try {
            return format.parse(string)
        } catch (e: ParseException) {
            return null
        }
    }

    fun getIntField(field: String, default: Int): Int {
        try {
            val string = fields[field]
            if (string == null) {
                // пустой строки в fields не может быть, мы это проверяем на set()
                return default
            }
            return string.toInt()
        } catch (e: NumberFormatException) {
            return default
        }
    }
}
