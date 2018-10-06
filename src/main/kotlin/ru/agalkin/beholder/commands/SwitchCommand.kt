package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.CommandException
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.conveyor.Step
import ru.agalkin.beholder.conveyor.StepResult

class SwitchCommand(app: Beholder, arguments: Arguments) : CommandAbstract(app, arguments) {
    private val template = arguments.shiftStringTemplateStrictSyntax("`switch` needs an argument")
    init {
        arguments.end()
    }

    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "case" -> {
                if (!hasDefaultBlock()) {
                    SwitchCaseCommand(app, args, template)
                } else {
                    throw CommandException("`switch` cannot have `case` subcommand after `default`")
                }
            }
            "default" -> {
                if (!hasDefaultBlock()) {
                    SwitchDefaultCommand(app, args)
                } else {
                    throw CommandException("`switch` cannot have multiple `default` subcommands")
                }
            }
            else -> null
        }

    private fun hasDefaultBlock()
        = subcommands.any { it is SwitchDefaultCommand }

    interface SwitchSubcommand {
        fun getConditionStep(): Step
    }

    private object SwitchNoopDefaultStep : Step {
        override fun execute(message: Message): StepResult
            = StepResult.CONTINUE

        override fun getDescription()
            = "implied noop default branch of switch"
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        val conditions = mutableListOf<Pair<Step, (Conveyor) -> Conveyor>>()
        for (subcommand in subcommands) {
            if (subcommand is SwitchSubcommand) {
                conditions.add(Pair(subcommand.getConditionStep(), subcommand::buildConveyor))
            }
        }
        if (!hasDefaultBlock()) {
            conditions.add(Pair(
                SwitchNoopDefaultStep,
                // Conveyor building function is a noop
                { conv -> conv }
            ))
        }

        return conveyor.addConditions(conditions)
    }
}
