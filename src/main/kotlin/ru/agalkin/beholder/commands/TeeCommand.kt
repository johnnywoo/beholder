package ru.agalkin.beholder.commands

import ru.agalkin.beholder.config.expressions.Arguments

open class TeeCommand(arguments: Arguments) : ConveyorCommandAbstract(
    arguments.end(),
    sendInputToOutput = true,
    sendInputToSubcommands = true,
    sendLastSubcommandToOutput = false
)
