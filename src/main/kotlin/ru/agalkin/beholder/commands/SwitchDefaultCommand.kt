package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments

class SwitchDefaultCommand(
    arguments: Arguments
) : ConveyorCommandAbstract(
    arguments.end(),
    sendInputToOutput = false,
    sendInputToSubcommands = true,
    sendLastSubcommandToOutput = true
), SwitchCommand.SwitchSubcommand {

    override fun inputIfMatches(message: Message): Boolean {
        input(message)
        return true
    }
}
