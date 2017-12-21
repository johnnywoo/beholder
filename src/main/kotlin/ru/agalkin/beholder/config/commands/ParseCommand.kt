package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.inflaters.BeholderStatsInflater
import ru.agalkin.beholder.inflaters.Inflater
import ru.agalkin.beholder.inflaters.SyslogInflater

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

    private val inflater: Inflater

    init {
        inflater = when (arguments.shiftString("We need some format to `parse`")) {
            "syslog" -> SyslogInflater()
            "beholder-stats" -> BeholderStatsInflater()
            else -> throw CommandException("Cannot understand arguments of `parse` command")
        }

        arguments.end()
    }

    override fun receiveMessage(message: Message) {
        inflater.inflateMessageFields(message)
        super.receiveMessage(message)
    }
}
