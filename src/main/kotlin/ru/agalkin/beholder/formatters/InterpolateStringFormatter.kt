package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class InterpolateStringFormatter(private val template: String) : Formatter {
    override fun formatMessage(message: Message)
        = template.replace(Regex("\\$([a-z0-9_]+)", RegexOption.IGNORE_CASE), {
            message.stringField(it.groups[1]?.value!!) ?: ""
        })
}
