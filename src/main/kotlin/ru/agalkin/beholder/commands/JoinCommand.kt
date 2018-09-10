package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.config.expressions.Arguments

open class JoinCommand(app: Beholder, arguments: Arguments) : ConveyorCommandAbstract(app, arguments.end()) {
    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        // join = сообщение из субкоманд прилетает в основной конвейер

        // Собираем конвейер для субкоманд
        var subconveyor = conveyor.createRelatedConveyor()
        for (subcommand in subcommands) {
            subconveyor = subcommand.buildConveyor(subconveyor)
        }

        // На выходе из субконвейера стоит основной конвейер
        subconveyor.terminateByMergingIntoInput(conveyor.addInput(getDefinition(includeSubcommands = false)))

        return conveyor
    }
}
