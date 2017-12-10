package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import java.lang.NumberFormatException
import java.util.regex.Pattern

class ParseCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |parse syslog;
            |
            |This command sets fields on messages according to chosen format.
            |
            |Format `syslog`: the only syslog variant currently supported is
            |a BSD-style syslog format as produced by nginx.
            |Incoming messages look like this:
            |<190>Nov 25 13:46:44 host nginx: <actual log message>
            |
            |Fields produced by `parse syslog`:
            |  ¥syslogFacility  -- numeric syslog facility
            |  ¥syslogSeverity  -- numeric syslog severity
            |  ¥syslogHost      -- source host from the message
            |  ¥syslogProgram   -- program name (nginx calls this "tag")
            |  ¥payload         -- actual log message (this would've been written to a file by nginx)
            |
            |If a message cannot be parsed, it will be left unchanged.
            |""".trimMargin().replace("¥", "$")
    }

    init {
        when (arguments.shift("We need some format to `parse`")) {
            "syslog" -> Unit
            else     -> throw CommandException("Cannot understand arguments of `parse` command")
        }

        arguments.end()
    }

    // <190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"
    private val syslogNginxRegex = Pattern.compile(
        """
            ^
            < (?<priority>[0-9]+) >
            (?: Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)
            \s+ (?: (?:\d)? \d) # day
            \s+ (?: \d\d:\d\d:\d\d) # time
            (?: \s+ (?<host> [^\s:]+) )? # 'nohostname' parameter in nginx
            \s+ (?<tag> [^\s:]+):
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

        val matcher = syslogNginxRegex.matcher(message.getPayload())
        if (!matcher.matches()) {
            super.emit(message)
            return
        }

        val priority = matcher.group("priority")
        if (priority != null) {
            try {
                val int = Integer.parseInt(priority)
                // The Priority value is calculated by first multiplying the Facility number by 8
                // and then adding the numerical value of the Severity.
                message["syslogFacility"] = (int / 8).toString()
                message["syslogSeverity"] = (int % 8).toString()
            } catch (ignored: NumberFormatException) {}
        }

        val host = matcher.group("host")
        if (host != null) {
            message["syslogHost"] = host
        }

        val tag = matcher.group("tag")
        if (tag != null) {
            message["syslogProgram"] = tag
        }

        val payload = matcher.group("payload")
        if (payload != null) {
            message["payload"] = payload
        }

        super.emit(message)
    }
}
