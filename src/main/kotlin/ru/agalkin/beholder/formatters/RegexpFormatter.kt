package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.RegexpToken
import ru.agalkin.beholder.getIsoDateFormatter
import java.net.InetAddress
import java.util.*
import java.util.regex.Pattern

class RegexpFormatter(private val regexp: Pattern, private val replacement: String, private val field: String) : Formatter {
    override fun formatMessage(message: Message)
        = regexp.matcher(message[field]).replaceAll(replacement)!!
}
