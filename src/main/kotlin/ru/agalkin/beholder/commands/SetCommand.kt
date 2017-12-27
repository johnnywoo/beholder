package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.formatters.*

class SetCommand(arguments: Arguments) : LeafCommandAbstract(arguments) {
    companion object {
        val help = """
            |set ¥field 'template with ¥fields from the message';
            |set ¥field <function> [... function args];
            |
            |This command manipulates individual message fields.
            |
            |When given a quoted string, `set` will substitute field names in the string
            |with corresponding values from the message.
            |
            |Example:
            |  flow {
            |      from timer {set ¥color 'red'}
            |      from timer {set ¥color 'green'}
            |      set ¥payload 'We got ¥color apples!';
            |      to stdout;
            |  }
            |
            |This example will produce messages like these:
            |  We got red apples!
            |  We got green apples!
            |  We got red apples!
            |  We got green apples!
            |
            |When given a built-in function, `set` can construct different values for message fields.
            |
            |Functions:
            |  syslog  -- Generates a IETF syslog payload based on syslog-related fields;
            |             see `parse syslog` for details.
            |  replace -- String replacement with regexp. See below.
            |  time    -- Current time, e.g. 01:23:45.
            |  dump    -- Generates a dump payload with all fields of the message.
            |
            |`set ¥field replace <regexp> <replacement> [in <subject>];`
            |Takes subject string, replaces all occurences of regexp in it with the replacement,
            |and stores the new value into ¥field. Default subject is ¥field itself.
            |Examples:
            |  `set ¥payload replace ~warn(ing)?~i 'WARNING';`
            |  `set ¥host replace ~^www\.~ '' in '¥subdomain.¥domain';`
            |Be aware of double-escaping in replacement strings!
            |Example:
            |  `set ¥payload replace ~\n~ '\\\\n';`
            |This command converts newlines into `\n` sequences.
            |""".trimMargin().replace("¥", "$")
    }

    private val field = arguments.shiftFieldName("First argument to `set` should be a field name (`set \$payload ...`)")
    private val formatter: Formatter

    init {
        // set $field function [...]
        // set $field 'interpolated-value'

        val arg = arguments.shiftStringToken("`set` needs at least two arguments")

        formatter = when ((arg as? LiteralToken)?.getValue()) {
            "syslog"  -> SyslogIetfFormatter()
            "dump"    -> DumpFormatter()
            "time"    -> TimeFormatter()
            "replace" -> ReplaceFormatter(
                arguments.shiftRegexp("`replace` needs a regexp"),
                arguments.shiftString("`replace` needs a replacement string"),
                arguments.shiftPrefixedStringOrNull(setOf("in"), "`replace ... in` needs a string") ?: "\$$field"
            )
            else -> TemplateFormatter.create(arg.getValue())
        }

        arguments.end()
    }

    override fun receiveMessage(message: Message) {
        message[field] = formatter.formatMessage(message)
        super.receiveMessage(message)
    }
}
