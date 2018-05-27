package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.inflaters.*
import ru.agalkin.beholder.stats.Stats

class ParseCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    private val inflater: Inflater
    private val shouldKeepUnparsed = arguments.shiftLiteralOrNull("keep-unparsed") != null

    init {
        val regexp = arguments.shiftRegexpOrNull()
        if (regexp != null) {
            inflater = RegexpInflater(regexp)
        } else {
            inflater = when (arguments.shiftAnyLiteral("We need some format to `parse`")) {
                "syslog" -> SyslogInflater()
                "json" -> JsonInflater()
                "beholder-stats" -> BeholderStatsInflater()
                "each-field-as-message" -> EachFieldAsMessageInflater("key", "value")
                else -> throw CommandException("Cannot understand arguments of `parse` command")
            }
        }

        arguments.end()
    }

    override fun stop() {
        if (inflater is BeholderStatsInflater) {
            inflater.stop()
        }
        super.stop()
    }

    override fun input(message: Message) {
        val success = inflater.inflateMessageFields(message) {
            output.sendMessageToSubscribers(it)
        }
        if (!success) {
            if (shouldKeepUnparsed) {
                output.sendMessageToSubscribers(message)
            } else {
                Stats.reportUnparsedDropped()
            }
        }
    }
}
