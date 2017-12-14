package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import java.util.regex.Pattern

class RegexpFormatter(private val regexp: Pattern, replacement: String, private val field: String) : Formatter {
    private val interpolateStringFormatter = InterpolateStringFormatter(replacement)

    override fun formatMessage(message: Message): String {
        val interpolatedReplacement = interpolateStringFormatter.formatMessage(message)
        return regexp.matcher(message[field]).replaceAll(interpolatedReplacement)!!
    }
}
