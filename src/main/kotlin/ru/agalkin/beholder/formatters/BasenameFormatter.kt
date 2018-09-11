package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.defaultString

class BasenameFormatter(private val template: TemplateFormatter) : Formatter {
    private val regex = "^~|^(\\.\\.|[.~|])$".toRegex()

    override fun formatMessage(message: Message): FieldValue {
        val fieldValue = template.formatMessage(message)
        val path = fieldValue.toString().dropLastWhile { it == '/' }
        val chunk = path.substring(path.lastIndexOfAny(charArrayOf('/', '\\', ':', ';')) + 1)
        val name = chunk.replace(regex, "")
        return FieldValue.fromString(defaultString(name, "noname"))
    }
}
