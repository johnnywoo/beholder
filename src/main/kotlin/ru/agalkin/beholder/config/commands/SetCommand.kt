package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.formatters.DumpFormatter
import ru.agalkin.beholder.formatters.Formatter
import ru.agalkin.beholder.formatters.InterpolateStringFormatter
import ru.agalkin.beholder.formatters.SyslogIetfFormatter

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
            |             see `parse syslog-nginx` for details.
            |  dump    -- Generates a dump payload with all fields of the message.
            |""".trimMargin().replace("¥", "$")
    }

    private val field = arguments.shiftFieldName("First argument to `set` should be a field name (`set \$payload ...`)")
    private val formatter: Formatter

    init {
        // set $field function [...]
        // set $field 'interpolated-value'

        val arg = arguments.shiftToken("`set` needs at least two arguments")

        formatter = when ((arg as? LiteralToken)?.getValue()) {
            "syslog" -> SyslogIetfFormatter()
            "dump"   -> DumpFormatter()
            else     -> InterpolateStringFormatter(arg.getValue())
        }

        arguments.end()
    }

    override fun emit(message: Message) {
        val newMessage = message.copy()

        newMessage[field] = formatter.formatMessage(newMessage)

        super.emit(newMessage)
    }
}
