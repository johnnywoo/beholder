package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments

class SwitchDefaultCommand(
    app: Beholder,
    arguments: Arguments
) : ConveyorCommandAbstract(app, arguments.end()), SwitchCommand.SwitchSubcommand {

    private lateinit var conveyorInput: Conveyor.Input

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        conveyorInput = conveyor.addInput()

        var currentConveyor = conveyor
        for (subcommand in subcommands) {
            currentConveyor = subcommand.buildConveyor(currentConveyor)
        }

        return currentConveyor
    }

    override fun inputIfMatches(message: Message): Boolean {
        conveyorInput.addMessage(message)
        return true
    }
}
