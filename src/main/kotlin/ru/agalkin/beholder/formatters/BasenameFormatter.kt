package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.defaultString

class BasenameFormatter(private val template: TemplateFormatter) : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        val fieldValue = template.formatMessage(message)
        val path = fieldValue.toString().dropLastWhile { it in charArrayOf('/') }
        val chunk = path.substring(path.lastIndexOfAny(charArrayOf('/', '\\', ':', ';')) + 1)
        val name = chunk.replace("^~|^(\\.\\.|[.~|])$".toRegex(), "")
        return FieldValue.fromString(defaultString(name, "noname"))
    }
}
