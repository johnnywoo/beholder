package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.expressions.Arguments

open class TeeCommand(app: Beholder, arguments: Arguments) : ConveyorCommandAbstract(
    app,
    arguments.end(),
    sendInputToOutput = true,
    sendInputToSubcommands = true,
    sendLastSubcommandToOutput = false
)
