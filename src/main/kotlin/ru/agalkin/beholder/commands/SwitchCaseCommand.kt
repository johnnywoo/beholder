package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.formatters.TemplateFormatter
import ru.agalkin.beholder.inflaters.RegexpInflater
import java.util.regex.Pattern

class SwitchCaseCommand(
    app: Beholder,
    arguments: Arguments,
    private val template: TemplateFormatter
) : ConveyorCommandAbstract(
    app,
    arguments,
    sendInputToOutput = false,
    sendInputToSubcommands = true,
    sendLastSubcommandToOutput = true
), SwitchCommand.SwitchSubcommand {
    private val matcher: Matcher
    init {
        val regexp = arguments.shiftRegexpOrNull()
        if (regexp != null) {
            matcher = RegexpMatcher(regexp)
        } else {
            matcher = ExactMatcher(arguments.shiftStringTemplate("`case` needs a regexp or a string"))
        }
        arguments.end()
    }

    override fun inputIfMatches(message: Message) = matcher.inputIfMatches(message)

    interface Matcher {
        fun inputIfMatches(message: Message): Boolean
    }

    private inner class RegexpMatcher(regexp: Pattern) : Matcher {
        private val regexpInflater = RegexpInflater(regexp, template)

        override fun inputIfMatches(message: Message): Boolean
            = regexpInflater.inflateMessageFields(message) { input(it) }
    }

    private inner class ExactMatcher(private val caseTemplate: TemplateFormatter) : Matcher {
        override fun inputIfMatches(message: Message): Boolean {
            if (caseTemplate.formatMessage(message).toString() == template.formatMessage(message).toString()) {
                input(message)
                return true
            }
            return false
        }
    }
}
