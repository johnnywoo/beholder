package ru.agalkin.beholder

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val createdMessagesCount = AtomicLong(0)

class Message {
    private val fields: MutableMap<String, FieldValue> = mutableMapOf()

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
            fields[field] = FieldValue.fromString(value)
        }
    }

    fun setFieldValue(field: String, value: FieldValue) {
        fields[field] = value
    }

    fun remove(field: String) {
        fields.remove(field)
    }

    fun getFieldNames()
        = fields.keys

    fun getPayload()
        = getStringField("payload")

    fun getStringField(field: String, default: String = "")
        = fields[field]?.toString() ?: default

    fun getFieldValue(field: String)
        = fields[field] ?: FieldValue.empty

    private var dateFormat: SimpleDateFormat? = null

    fun getDateField(field: String): Date? {
        var format = dateFormat
        if (format == null) {
            format = getIsoDateFormatter()
            dateFormat = format
        }
        val fieldValue = fields[field]
        if (fieldValue == null) {
            return null
        }
        try {
            return format.parse(fieldValue.toString())
        } catch (e: ParseException) {
            return null
        }
    }

    fun getIntField(field: String, default: Int): Int {
        try {
            val fieldValue = fields[field]
            if (fieldValue == null) {
                // пустой строки в fields не может быть, мы это проверяем на set()
                return default
            }
            return fieldValue.toString().toInt()
        } catch (e: NumberFormatException) {
            return default
        }
    }
}
