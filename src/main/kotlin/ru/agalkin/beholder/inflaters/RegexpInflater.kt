package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.CommandException
import java.util.regex.Pattern

class RegexpInflater(private val regexp: Pattern) : Inflater {
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

    override fun inflateMessageFields(message: Message) {
        val matcher = regexp.matcher(message.getPayload())
        if (!matcher.find()) {
            return
        }
        for (name in groupNames) {
            val value = matcher.group(name)
            if (value != null) {
                message[name] = value
            }
        }
    }
}
