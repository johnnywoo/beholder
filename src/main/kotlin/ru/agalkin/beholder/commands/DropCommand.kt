package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.conveyor.Conveyor

class DropCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    init {
        arguments.end()
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        val nextConveyor = conveyor.createRelatedConveyor()

        // There is a bug somewhere in the conveyour that makes `drop` command
        // break all sorts of things around conditional instructions.
        // This seems to fix it temporarily while we're working on a less magical conveyour design.
        nextConveyor.addInput("please rewrite the conveyour again")

        return nextConveyor
    }
}
