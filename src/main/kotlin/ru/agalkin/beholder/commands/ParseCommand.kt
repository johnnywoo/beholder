package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.conveyor.ConveyorInput
import ru.agalkin.beholder.conveyor.Step
import ru.agalkin.beholder.conveyor.StepResult
import ru.agalkin.beholder.inflaters.*
import ru.agalkin.beholder.stats.Stats

class ParseCommand(app: Beholder, arguments: Arguments) : LeafCommandAbstract(app, arguments) {
    private val inflater: Inflater
    private val shouldKeepUnparsed = arguments.shiftLiteralOrNull("keep-unparsed") != null

    init {
        val regexp = arguments.shiftRegexpOrNull()
        if (regexp != null) {
            inflater = RegexpInflater(regexp)
        } else {
            inflater = when (arguments.shiftAnyLiteral("We need some format to `parse`")) {
                "syslog" -> SyslogInflater()
                "json" -> JsonInflater()
                "fieldpack" -> FieldpackInflater()
                "beholder-stats" -> BeholderStatsInflater()
                "each-field-as-message" -> EachFieldAsMessageInflater("key", "value")
                else -> throw CommandException("Cannot understand arguments of `parse` command")
            }
        }

        arguments.end()
    }

    override fun stop() {
        if (inflater is BeholderStatsInflater) {
            inflater.stop()
        }
        super.stop()
    }

    private inner class ParseInplaceStep : Step {
        override fun execute(message: Message): StepResult {
            if (!(inflater as InplaceInflater).inflateMessageFieldsInplace(message) && !shouldKeepUnparsed) {
                Stats.reportUnparsedDropped()
                return StepResult.DROP
            }
            return StepResult.CONTINUE
        }

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    private inner class ParseEmitStep(private val input: ConveyorInput) : Step {
        override fun execute(message: Message): StepResult {
            val success = inflater.inflateMessageFields(message) {
                input.addMessage(it)
            }
            if (!success) {
                if (shouldKeepUnparsed) {
                    input.addMessage(message)
                } else {
                    Stats.reportUnparsedDropped()
                }
            }
            return StepResult.DROP
        }

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        if (inflater is InplaceInflater) {
            // Не добавляем лишних телодвижений, если тут не может появиться второго сообщения
            conveyor.addStep(ParseInplaceStep())
            return conveyor
        }

        val nextConveyor = conveyor.createRelatedConveyor()

        conveyor.addStep(ParseEmitStep(nextConveyor.addInput(getDefinition(includeSubcommands = false))))

        return nextConveyor
    }
}
