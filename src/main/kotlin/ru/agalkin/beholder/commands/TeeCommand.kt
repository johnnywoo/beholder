package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.conveyor.Conveyor

open class TeeCommand(app: Beholder, arguments: Arguments) : ConveyorCommandAbstract(app, arguments.end()) {
    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        // tee = раздваиваем сообщение, одно уходит просто в субкоманды, другое просто на выход
        val subconveyor = conveyor.createRelatedConveyor()
        conveyor.copyToConveyor(subconveyor, getDefinition(includeSubcommands = false))

        var currentConveyor = subconveyor
        for (subcommand in subcommands) {
            currentConveyor = subcommand.buildConveyor(currentConveyor)
        }

        return conveyor
    }
}
