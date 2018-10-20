package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.conveyor.Step
import ru.agalkin.beholder.conveyor.StepResult
import ru.agalkin.beholder.formatters.DumpFormatter
import ru.agalkin.beholder.formatters.TemplateFormatter

class DumpCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    private val dumpStep: DumpStep
    init {
        val prefix = if (arguments.hasMoreTokens()) arguments.shiftStringTemplateStrictSyntax("") else null
        dumpStep = DumpStep(prefix)
        arguments.end()
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        conveyor.addStep(dumpStep)
        return conveyor
    }

    private class DumpStep(val prefix: TemplateFormatter?) : Step {
        override fun execute(message: Message): StepResult {
            val formatter = DumpFormatter(prefix?.formatMessage(message)?.toString())
            print(formatter.formatMessage(message).withNewlineAtEnd().toString())
            return StepResult.CONTINUE
        }
    }
}
