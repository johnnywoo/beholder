package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments

class SwitchDefaultCommand(
    app: Beholder,
    arguments: Arguments
) : ConveyorCommandAbstract(app, arguments.end()), SwitchCommand.SwitchSubcommand {
    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        var currentConveyor = conveyor
        for (subcommand in subcommands) {
            currentConveyor = subcommand.buildConveyor(currentConveyor)
        }

        return currentConveyor
    }

    private inner class SwitchDefaultStep : Conveyor.Step {
        override fun execute(message: Message): Conveyor.StepResult
            = Conveyor.StepResult.CONTINUE

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    override fun getConditionStep(): Conveyor.Step {
        return SwitchDefaultStep()
    }
}
