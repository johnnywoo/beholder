package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import java.lang.NumberFormatException
import java.util.regex.Pattern

class ParseCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |parse syslog;
            |parse beholder-stats;
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
            |
            |Format `beholder-stats`: fills the message with internal Beholder stats.
            |Use this with `from timer` to create a health log.
            |
            |Fields produced by `parse beholder-stats`:
            |  ¥uptimeSeconds  -- Uptime in seconds
            |  ¥heapBytes      -- Current heap size in bytes (memory usage)
            |  ¥heapUsedBytes  -- Used memory in the heap
            |  ¥heapMaxBytes   -- Maximal heap size
            |  ¥payload        -- A summary of Beholder stats
            |""".trimMargin().replace("¥", "$")
    }

    private val parser: Parser

    init {
        parser = when (arguments.shiftString("We need some format to `parse`")) {
            "syslog" -> SyslogParser()
            "beholder-stats" -> BeholderStatsParser()
            else -> throw CommandException("Cannot understand arguments of `parse` command")
        }

        arguments.end()
    }

    override fun receiveMessage(message: Message) {
        parser.processMessage(message)
        super.receiveMessage(message)
    }


    private interface Parser {
        fun processMessage(message: Message)
    }

    private class BeholderStatsParser : Parser {
        override fun processMessage(message: Message) {
            val runtime = Runtime.getRuntime()

            val heapSize   = runtime.totalMemory()
            val heapMax    = runtime.maxMemory()
            val heapUnused = runtime.freeMemory()

            val heapUsed = heapSize - heapUnused

            val uptimeDate = Beholder.uptimeDate
            val uptimeSeconds = when (uptimeDate) {
                null -> 0
                else -> ((System.currentTimeMillis() - uptimeDate.time) / 1000).toInt()
            }

            message["uptimeSeconds"] = uptimeSeconds.toString()
            message["heapBytes"]     = heapSize.toString()
            message["heapUsedBytes"] = heapUsed.toString()
            message["heapMaxBytes"]  = heapMax.toString()
            message["payload"]       = "heap ${getMemoryString(heapSize)} heap-used ${getMemoryString(heapUsed)} heap-max ${getMemoryString(heapMax)} uptime ${getUptimeString(uptimeSeconds)}"
        }

        private val uptimeUnits = mapOf(
            24 * 60 * 60 to "d",
            60 * 60 to "h",
            60 to "m"
        )

        private fun getUptimeString(uptimeSeconds: Int): String {
            val sb = StringBuilder()
            var seconds = uptimeSeconds

            for ((unitSize, letter) in uptimeUnits) {
                if (seconds >= unitSize) {
                    sb.append(seconds / unitSize).append(letter)
                    seconds = seconds.rem(unitSize)
                }
            }

            if (sb.isEmpty() || seconds > 0) {
                sb.append(seconds).append("s")
            }

            return sb.toString()
        }

        private val memoryUnits = mapOf(
            1024 * 1024 * 1024 to "G",
            1024 * 1024 to "M",
            1024 to "K"
        )

        private fun getMemoryString(bytesNum: Long): String {
            for ((unitSize, letter) in memoryUnits) {
                if (bytesNum >= unitSize) {
                    val n = bytesNum.toFloat() / unitSize
                    return String.format(if (n > 99) "%.0f" else "%.1f", n).replace(Regex("\\.0$"), "") + letter
                }
            }
            return bytesNum.toString()
        }
    }

    private class SyslogParser : Parser {
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

        override fun processMessage(message: Message) {
            // мы тут хотим разобрать формат старого сислога и сложить данные из него в теги
            // формат старого сислога:
            // <190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"

            val matcher = syslogNginxRegex.matcher(message.getPayload())
            if (!matcher.matches()) {
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
        }
    }
}
