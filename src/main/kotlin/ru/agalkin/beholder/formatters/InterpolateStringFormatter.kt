package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class InterpolateStringFormatter(private val template: String) : Formatter {
    private val regex = Regex("\\$([a-z][a-z0-9_]*)", RegexOption.IGNORE_CASE)

    // todo тут можно нащупать, что переменных никаких нет, и просто возвращать template

    override fun formatMessage(message: Message): String
        = template.replace(regex, { message[it.groups[1]?.value!!] ?: "" })
}
