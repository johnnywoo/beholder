package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
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

    private inner class ParseInplaceStep : Conveyor.Step {
        override fun execute(message: Message): Conveyor.StepResult {
            if (!(inflater as InplaceInflater).inflateMessageFieldsInplace(message) && !shouldKeepUnparsed) {
                Stats.reportUnparsedDropped()
                return Conveyor.StepResult.DROP
            }
            return Conveyor.StepResult.CONTINUE
        }

        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    private inner class ParseEmitStep(private val input: Conveyor.Input) : Conveyor.Step {
        override fun execute(message: Message): Conveyor.StepResult {
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
            return Conveyor.StepResult.DROP
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
