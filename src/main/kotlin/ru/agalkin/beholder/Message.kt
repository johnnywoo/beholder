package ru.agalkin.beholder

import ru.agalkin.beholder.formatters.TimeFormatter
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicLong

private val createdMessagesCount = AtomicLong(0)

class Message(initialFields: Map<String, FieldValue>? = null) {
    private var fields = if (initialFields == null) HashMap() else HashMap(initialFields)

    val messageId = createdMessagesCount.getAndIncrement()

    fun copy()
        = Message(fields)

    operator fun set(field: String, value: String) {
        if (value.isEmpty()) {
            fields.remove(field)
        } else {
            fields[field] = FieldValue.fromString(value)
        }
    }

    fun setFieldValue(field: String, value: FieldValue) {
        if (value.getByteLength() == 0) {
            fields.remove(field)
        } else {
            fields[field] = value
        }
    }

    fun remove(field: String) {
        fields.remove(field)
    }

    fun getFieldNames(): Set<String>
        = fields.keys

    fun getPayloadString()
        = getPayloadValue().toString()

    fun getPayloadValue()
        = getFieldValue("payload")

    fun getStringField(field: String, default: String = "")
        = fields[field]?.toString() ?: default

    fun getFieldValue(field: String)
        = fields[field] ?: FieldValue.empty

    fun getDateField(field: String): ZonedDateTime?
        = TimeFormatter.parseDate(fields[field].toString())

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

    companion object {
        fun of(vararg pairs: Pair<String, String>): Message {
            val message = Message()
            for (pair in pairs) {
                message[pair.first] = pair.second
            }
            return message
        }
    }
}
