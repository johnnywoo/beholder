package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
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

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        if (inflater is InplaceInflater) {
            // Не добавляем лишних телодвижений, если тут не может появиться второго сообщения
            conveyor.addStep { message ->
                if (!inflater.inflateMessageFieldsInplace(message) && !shouldKeepUnparsed) {
                    Stats.reportUnparsedDropped()
                    return@addStep Conveyor.StepResult.DROP
                }
                Conveyor.StepResult.CONTINUE
            }
            return conveyor
        }

        val nextConveyor = conveyor.createRelatedConveyor()
        val input = nextConveyor.addInput()

        conveyor.addStep { message ->
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
            return@addStep Conveyor.StepResult.DROP
        }

        return nextConveyor
    }
}
