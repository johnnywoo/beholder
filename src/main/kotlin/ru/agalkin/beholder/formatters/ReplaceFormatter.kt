package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import java.util.regex.Pattern

class ReplaceFormatter(private val regexp: Pattern, replacement: String, private val field: String) : Formatter {
    private val interpolateStringFormatter = InterpolateStringFormatter(replacement)

    override fun formatMessage(message: Message): String {
        val interpolatedReplacement = interpolateStringFormatter.formatMessage(message)
        val fieldValue = message.getStringField(field)
        try {
            return regexp.matcher(fieldValue).replaceAll(interpolatedReplacement) ?: fieldValue
        } catch (e: Throwable) {
            // не получилось по каким-то причинам совершить замену
            // скорее всего ошибка в строке замены
            InternalLog.exception(e)
            return fieldValue
        }
    }
}
