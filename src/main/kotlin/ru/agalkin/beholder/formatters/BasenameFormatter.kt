package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.defaultString

class BasenameFormatter(private val template: TemplateFormatter) : Formatter {
    override fun formatMessage(message: Message): String {
        val path = template.formatMessage(message).dropLastWhile { it in charArrayOf('/') }
        val chunk = path.substring(path.lastIndexOfAny(charArrayOf('/', '\\', ':', ';')) + 1)
        val name = chunk.replace("^~|^(\\.\\.|[.~|])$".toRegex(), "")
        return defaultString(name, "noname")
    }
}
