package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract

class KeepCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    private val fieldsToKeep: Set<String>

    init {
        // keep $field [$field ...]

        val fieldNames = mutableSetOf<String>()

        fieldNames.add(arguments.shiftFieldName("`keep` needs at least one field name"))

        while (arguments.hasMoreTokens()) {
            fieldNames.add(arguments.shiftFieldName("All arguments of `keep` must be field names"))
        }

        arguments.end()

        fieldsToKeep = fieldNames
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        conveyor.addStep { message ->
            for (field in message.getFieldNames().minus(fieldsToKeep)) {
                message.remove(field)
            }
            return@addStep Conveyor.StepResult.CONTINUE
        }
        return conveyor
    }
}
