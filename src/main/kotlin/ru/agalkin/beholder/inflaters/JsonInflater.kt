package ru.agalkin.beholder.inflaters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import ru.agalkin.beholder.Message

class JsonInflater : Inflater {
    private val gson = Gson()

    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        val data: JsonObject?
        try {
            data = gson.fromJson(message.getPayloadString(), JsonObject::class.java)
        } catch (e: JsonSyntaxException) {
            return false
        }

        if (data == null) {
            return false
        }

        // we only allow scalar/null values here
        for (field in data.keySet()) {
            val value = data.get(field)
            if (!value.isJsonNull && !value.isJsonPrimitive) {
                return false
            }
        }

        // ok, everything is scalar/null, setting message fields
        for (field in data.keySet()) {
            val value = data.get(field)
            if (value.isJsonNull) {
                message.remove(field)
                continue
            }

            message[field] = value.asJsonPrimitive.asString
        }

        emit(message)
        return true
    }
}
