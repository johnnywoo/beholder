package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.formatters.*

class SetCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    private val field = arguments.shiftFieldName("First argument to `set` should be a field name (`set \$payload ...`)")
    private val formatter: Formatter

    init {
        // set $field function [...]
        // set $field 'interpolated-value'

        val arg = arguments.shiftAnyLiteralOrNull()
        formatter = when (arg) {
            "syslog" -> SyslogIetfFormatter()
            "prefix-with-length" -> PrefixWithLengthFormatter()
            "dump" -> DumpFormatter()
            "time" -> TimeFormatter()
            "host" -> HostFormatter()
            "env" -> EnvFormatter(arguments.shiftFixedString("`set ... env` needs an environment variable name"))
            "json" -> JsonFormatter(nullIfEmpty(scanArgumentsAsFieldNames(arguments, "`set ... json` arguments must be field names")))
            "replace" -> {
                val regexp = arguments.shiftRegexp("`replace` needs a regexp")
                val replacementTemplate = arguments.shiftStringTemplate("`replace` needs a replacement string")

                val subjectTemplate: TemplateFormatter
                if (arguments.shiftLiteralOrNull("in") != null) {
                    subjectTemplate = arguments.shiftStringTemplate("`replace ... in` needs a string")
                } else {
                    subjectTemplate = TemplateFormatter.create("\$$field")
                }

                ReplaceFormatter(regexp, replacementTemplate, subjectTemplate)
            }
            null -> arguments.shiftStringTemplate("`set` needs at least two arguments")
            else -> TemplateFormatter.create(arg)
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

    override fun input(message: Message) {
        message[field] = formatter.formatMessage(message)
        output.sendMessageToSubscribers(message)
    }
}
