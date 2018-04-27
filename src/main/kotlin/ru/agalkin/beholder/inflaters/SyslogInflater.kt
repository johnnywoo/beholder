package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message
import java.lang.NumberFormatException
import java.util.regex.Pattern

class SyslogInflater : Inflater {
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
        """,
        Pattern.COMMENTS
    )

    override fun inflateMessageFields(message: Message): Boolean {
        // мы тут хотим разобрать формат старого сислога и сложить данные из него в теги
        // формат старого сислога:
        // <190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"

        val payload = message.getPayload()
        val matcher = syslogNginxRegex.matcher(payload)
        if (!matcher.find()) {
            return parseIetfSyslog(message)
        }

        val priority = matcher.group("priority")
        if (priority != null) {
            try {
                val int = Integer.parseInt(priority)
                // The Priority value is calculated by first multiplying the Facility number by 8
                // and then adding the numerical value of the Severity.
                message["facility"] = (int / 8).toString()
                message["severity"] = (int % 8).toString()
            } catch (ignored: NumberFormatException) {}
        }

        val host = matcher.group("host")
        if (host != null) {
            message["host"] = host
        }

        val tag = matcher.group("tag")
        if (tag != null) {
            message["program"] = tag
        }

        val headerLength = matcher.end()
        message["payload"] = payload.substring(headerLength)

        return true
    }

    // <15>1 2018-04-27T17:49:03+03:00 hostname program 73938 - - Message
    private val syslogIetfRegex = Pattern.compile(
        """
            ^
            < (?<priority>[0-9]+) >1
            \s (?<date> \d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d (?: Z | [+-]\d\d:\d\d) )
            \s (?<host> [^\s]+)
            \s (?<program> [^\s]+)
            \s (?<pid> [^\s]+)
            \s (?<messageId> [^\s]+)
            \s [^\s]+
            \s
        """,
        Pattern.COMMENTS
    )

    private fun parseIetfSyslog(message: Message): Boolean {
        // попробуем формат IETF-сислога
        // пока что поддерживается не всё: structured data должна быть - или хотя бы не содержать пробелов
        val payload = message.getPayload()
        val matcher = syslogIetfRegex.matcher(payload)
        if (!matcher.find()) {
            return false
        }

        val priority = matcher.group("priority")
        if (priority != null) {
            try {
                val int = Integer.parseInt(priority)
                // The Priority value is calculated by first multiplying the Facility number by 8
                // and then adding the numerical value of the Severity.
                message["facility"] = (int / 8).toString()
                message["severity"] = (int % 8).toString()
            } catch (ignored: NumberFormatException) {}
        }

        val date = matcher.group("date")
        if (date != null) {
            message["date"] = date.replace(Regex("Z$"), "+00:00")
        }

        val host = matcher.group("host")
        if (host != null) {
            message["host"] = host
        }

        val tag = matcher.group("program")
        if (tag != null) {
            message["program"] = tag
        }

        val pid = matcher.group("pid")
        if (pid != null) {
            message["pid"] = pid
        }

        val headerLength = matcher.end()
        message["payload"] = payload.substring(headerLength)

        return true
    }
}
