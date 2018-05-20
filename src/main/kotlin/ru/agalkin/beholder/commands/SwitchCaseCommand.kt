package ru.agalkin.beholder.commands

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
) {
    val regexpInflater = RegexpInflater(arguments.shiftRegexp("`case` needs a regexp"), template)
    init {
        arguments.end()
    }
}
