package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.formatters.*

class SetCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    private val field = arguments.shiftFieldName("First argument to `set` should be a field name (`set \$payload ...`)")
    private val formatter: Formatter

    init {
        // set $field function [...]
        // set $field 'interpolated-value'

        val arg = arguments.shiftStringToken("`set` needs at least two arguments")

        formatter = when ((arg as? LiteralToken)?.getValue()) {
            "syslog" -> SyslogIetfFormatter()
            "prefix-with-length" -> PrefixWithLengthFormatter()
            "dump" -> DumpFormatter()
            "time" -> TimeFormatter()
            "host" -> HostFormatter()
            "env" -> EnvFormatter(arguments.shiftString("`set ... env` needs an environment variable name"))
            "json" -> JsonFormatter(nullIfEmpty(scanArgumentsAsFieldNames(arguments, "`set ... json` arguments must be field names")))
            "replace" -> ReplaceFormatter(
                arguments.shiftRegexp("`replace` needs a regexp"),
                arguments.shiftString("`replace` needs a replacement string"),
                arguments.shiftPrefixedStringOrNull(setOf("in"), "`replace ... in` needs a string") ?: "\$$field"
            )
            else -> TemplateFormatter.create(arg.getValue())
        }

        arguments.end()
    }

    private fun scanArgumentsAsFieldNames(arguments: Arguments, errorMessage: String): List<String> {
        val fields = mutableListOf<String>()
        while (arguments.hasMoreTokens()) {
            fields.add(arguments.shiftFieldName(errorMessage))
        }
        return fields
    }

    private fun <T> nullIfEmpty(list: List<T>): List<T>? {
        return if(list.isEmpty()) null else list
    }

    override fun receiveMessage(message: Message) {
        message[field] = formatter.formatMessage(message)
        super.receiveMessage(message)
    }
}
