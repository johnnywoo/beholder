package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.formatters.TemplateFormatter
import java.util.regex.Pattern

class RegexpInflater(private val regexp: Pattern, private val template: TemplateFormatter = TemplateFormatter.payloadFormatter) : Inflater {
    private val groupNames: Set<String>

    init {
        if (regexp.flags() and Pattern.COMMENTS != 0) {
            throw CommandException("Using 'x' modifier in `parse ~regexp~` is not supported")
        }
        val names = mutableSetOf<String>()
        val matcher = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regexp.pattern())
        while (matcher.find()) {
            names.add(matcher.group(1))
        }
        groupNames = names
    }

    override fun inflateMessageFields(message: Message): Boolean {
        val matcher = regexp.matcher(template.formatMessage(message))
        if (!matcher.find()) {
            return false
        }
        for (name in groupNames) {
            val value = matcher.group(name)
            if (value != null) {
                message[name] = value
            }
        }

        return true
    }
}
