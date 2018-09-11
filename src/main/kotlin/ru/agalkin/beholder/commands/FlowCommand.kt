package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.conveyor.Conveyor

open class FlowCommand(app: Beholder, arguments: Arguments) : ConveyorCommandAbstract(app, arguments.end()) {
    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        // flow = вход летит на выход, субкоманды сами по себе

        // Субкоманды просто получают свой конвейер
        var currentConveyor = conveyor.createRelatedConveyor()
        for (subcommand in subcommands) {
            currentConveyor = subcommand.buildConveyor(currentConveyor)
        }

        // Оригинальный конвейер не трогаем
        return conveyor
    }
}
