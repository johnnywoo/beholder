package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ArgumentToken
import java.lang.NumberFormatException
import java.util.regex.Pattern

class ParseCommand(arguments: List<ArgumentToken>) : LeafCommandAbstract(arguments) {
    init {
        when (requireArg(1, "We need some format to `parse`")) {
            "syslog-nginx" -> Unit
            else -> throw CommandException("Cannot understand arguments of `parse` command")
        }

        requireNoArgsAfter(1)
    }

    // <190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"
    private val syslogNginxRegex = Pattern.compile(
        """
            ^
            < (?<priority>[0-9]+) >
            (?<month>       Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)
            \s+ (?<day>     \d\d?)
            \s+ (?<time>    \d\d:\d\d:\d\d)
            \s+ (?<host>    [^\s]+)
            \s+ (?<program> [^\s]+):
            \s # one space
            (?<payload>   .*)
            $
        """,
        Pattern.COMMENTS
    )

    override fun emit(message: Message) {
        // мы тут хотим разобрать формат старого сислога и сложить данные из него в теги
        // формат старого сислога:
        // <190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"

        val newMessage = message.copy()

        val matcher = syslogNginxRegex.matcher(message.text)
        if (!matcher.matches()) {
            newMessage.tags["fail"] = "yep"
            super.emit(newMessage)
            return
        }

        val priority = matcher.group("priority")
        if (priority != null) {
            try {
                val int = Integer.parseInt(priority)
                newMessage.tags["syslogFacility"] = (int % 8).toString()
                newMessage.tags["syslogSeverity"] = (int / 8).toString()
            } catch (ignored: NumberFormatException) {}
        }

        val host = matcher.group("host")
        if (host != null) {
            newMessage.tags["syslogHost"] = host
        }

        val tag = matcher.group("program")
        if (tag != null) {
            newMessage.tags["syslogProgram"] = tag
        }

        val payload = matcher.group("payload")
        if (payload != null) {
            newMessage.tags["payload"] = payload
        }

        super.emit(newMessage)
    }
}
