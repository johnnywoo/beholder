package ru.agalkin.beholder.formatters

import com.google.gson.JsonObject
import ru.agalkin.beholder.Message

class JsonFormatter(private val fields: List<String>?) : Formatter {
    override fun formatMessage(message: Message): String {
        val keys = fields ?: message.getFields().keys.toList()

        val jsonObject = JsonObject()
        for (key in keys) {
            jsonObject.addProperty(key, message.getStringField(key, ""))
        }
        return jsonObject.toString()
    }
}
