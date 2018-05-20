package ru.agalkin.beholder.commands

import ru.agalkin.beholder.config.expressions.Arguments

open class JoinCommand(arguments: Arguments) : ConveyorCommandAbstract(
    arguments.end(),
    sendInputToOutput = true,
    sendInputToSubcommands = false,
    sendLastSubcommandToOutput = true
)
