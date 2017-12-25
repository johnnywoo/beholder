package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.inflaters.BeholderStatsInflater
import ru.agalkin.beholder.inflaters.Inflater
import ru.agalkin.beholder.inflaters.RegexpInflater
import ru.agalkin.beholder.inflaters.SyslogInflater

class ParseCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |parse syslog;
            |parse ~regexp-with-named-groups~;
            |parse beholder-stats;
            |
            |This command sets fields on messages according to chosen format.
            |If a message cannot be parsed, it will be left unchanged.
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
            |Format `~regexp-with-named-groups~`: if the regexp matches, named groups from it
            |become message fields. Group names should not be prefixed with ¥.
            |Example:
            |`parse ~(?<logKind>access|error)~;`
            |This will produce field ¥logKind with either 'access' or 'error' as value,
            |if either word occurs in ¥payload. If both words are present, earliest match is used.
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

    private val inflater: Inflater

    init {
        val regexp = arguments.shiftRegexpOrNull()
        if (regexp != null) {
            inflater = RegexpInflater(regexp)
        } else {
            inflater = when (arguments.shiftString("We need some format to `parse`")) {
                "syslog" -> SyslogInflater()
                "beholder-stats" -> BeholderStatsInflater()
                else -> throw CommandException("Cannot understand arguments of `parse` command")
            }
        }

        arguments.end()
    }

    override fun receiveMessage(message: Message) {
        inflater.inflateMessageFields(message)
        super.receiveMessage(message)
    }
}
