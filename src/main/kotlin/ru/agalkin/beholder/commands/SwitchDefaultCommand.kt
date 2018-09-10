package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.conveyor.Step
import ru.agalkin.beholder.conveyor.StepResult

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

    private inner class SwitchDefaultStep : Step {
        override fun execute(message: Message): StepResult
            = StepResult.CONTINUE

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    override fun getConditionStep(): Step {
        return SwitchDefaultStep()
    }
}
