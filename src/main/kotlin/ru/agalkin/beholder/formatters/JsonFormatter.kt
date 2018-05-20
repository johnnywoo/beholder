package ru.agalkin.beholder.formatters

import com.google.gson.JsonObject
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

class JsonFormatter(private val fields: List<String>?) : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        val keys = fields ?: message.getFieldNames().sorted()

        val jsonObject = JsonObject()
        for (key in keys) {
            jsonObject.addProperty(key, message.getStringField(key))
        }
        return FieldValue.fromString(jsonObject.toString())
    }
}
