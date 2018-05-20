package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.formatters.TemplateFormatter
import ru.agalkin.beholder.inflaters.RegexpInflater

class SwitchCaseCommand(
    arguments: Arguments,
    template: TemplateFormatter
) : ConveyorCommandAbstract(
    arguments,
    sendInputToOutput = false,
    sendInputToSubcommands = true,
    sendLastSubcommandToOutput = true
), SwitchCommand.SwitchSubcommand {

    val regexpInflater = RegexpInflater(arguments.shiftRegexp("`case` needs a regexp"), template)
    init {
        arguments.end()
    }

    override fun inputIfMatches(message: Message): Boolean {
        return regexpInflater.inflateMessageFields(message) {
            input(it)
        }
    }
}
