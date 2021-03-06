package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.TemplateParser
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.conveyor.Step
import ru.agalkin.beholder.conveyor.StepResult
import ru.agalkin.beholder.formatters.*

class SetCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    private val field = arguments.shiftFieldName("First argument to `set` should be a field name (`set \$payload ...`)")
    private val formatter: Formatter

    init {
        // set $field function [...]
        // set $field 'interpolated-value'

        val arg = arguments.shiftAnyLiteralOrNull()
        formatter = when (arg) {
            "syslog" -> SyslogIetfFormatter(app)
            "prefix-with-length" -> SyslogFrameFormatter()
            "syslog-frame" -> SyslogFrameFormatter()
            "dump" -> DumpFormatter()
            "time", "date" -> {
                val format: TimeFormatter.Format
                if (arguments.shiftLiteralOrNull("as") != null) {
                    val literal = arguments.shiftAnyLiteralOrNull()
                    if (literal != null) {
                        val namedFormat = TimeFormatter.getNamedFormat(literal)
                        if (namedFormat != null) {
                            format = namedFormat
                        } else {
                            format = TimeFormatter.getSimpleDateFormat(literal)
                        }
                    } else {
                        val string = arguments.shiftFixedString("`set ... $arg as` needs a string")
                        format = TimeFormatter.getSimpleDateFormat(string)
                    }
                } else {
                    format = when (arg) {
                        "time" -> TimeFormatter.FORMAT_TIME
                        "date" -> TimeFormatter.FORMAT_DATE
                        else -> throw BeholderException("Impossible")
                    }
                }

                var dateSource: TemplateFormatter? = null
                if (arguments.shiftLiteralOrNull("in") != null) {
                    dateSource = arguments.shiftStringTemplateStrictSyntax("`set ... $arg in` needs a string")
                }

                TimeFormatter(format, dateSource)
            }
            "host" -> HostFormatter()
            "env" -> EnvFormatter(arguments.shiftFixedString("`set ... env` needs an environment variable name"))
            "basename" -> BasenameFormatter(arguments.shiftStringTemplateStrictSyntax("`set ... basename` needs a file path"))
            "severity-name" -> SeverityNameFormatter(
                arguments.shiftStringTemplateStrictSyntax("`set ... severity-name` needs a severity number"),
                arguments.shiftLiteralOrNull("lowercase") != null
            )
            "json" -> JsonFormatter(nullIfEmpty(scanArgumentsAsFieldNames(arguments, "`set ... json` arguments must be field names")))
            "fieldpack" -> FieldpackFormatter(nullIfEmpty(scanArgumentsAsFieldNames(arguments, "`set ... fieldpack` arguments must be field names")))
            "replace" -> {
                val regexp = arguments.shiftRegexp("`replace` needs a regexp")
                val replacementTemplate = arguments.shiftStringTemplateForgivingSyntax("`replace` needs a replacement string")

                val subjectTemplate: TemplateFormatter
                if (arguments.shiftLiteralOrNull("in") != null) {
                    subjectTemplate = arguments.shiftStringTemplateStrictSyntax("`replace ... in` needs a string")
                } else {
                    subjectTemplate = TemplateFormatter.ofField(field)
                }

                ReplaceFormatter(regexp, replacementTemplate, subjectTemplate)
            }
            null -> arguments.shiftStringTemplateStrictSyntax("`set` needs at least two arguments")
            else -> TemplateFormatter.create(TemplateParser.parse(arg, false, false)) // from an unknown literal
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

    private inner class SetStep : Step {
        override fun execute(message: Message): StepResult {
            message.setFieldValue(field, formatter.formatMessage(message))
            return StepResult.CONTINUE
        }

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        conveyor.addStep(SetStep())
        return conveyor
    }
}
