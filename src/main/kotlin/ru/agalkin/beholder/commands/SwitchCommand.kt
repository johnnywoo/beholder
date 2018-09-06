package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.CommandException

class SwitchCommand(app: Beholder, arguments: Arguments) : CommandAbstract(app, arguments) {
    private val template = arguments.shiftStringTemplate("`switch` needs an argument")
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
        fun inputIfMatches(message: Message): Boolean
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        val outputConveyor = conveyor.createRelatedConveyor()

        if (!subcommands.isEmpty()) {
            val output = outputConveyor.addInput()

            for (subcommand in subcommands) {
                if (subcommand is SwitchSubcommand) {
                    subcommand
                        .buildConveyor(conveyor.createRelatedConveyor())
                        .terminateByMergingIntoInput(output)
                }
            }
        }

        conveyor.addStep { message ->
            for (subcommand in subcommands) {
                if (subcommand is SwitchSubcommand && subcommand.inputIfMatches(message)) {
                    break
                }
            }
            return@addStep Conveyor.StepResult.DROP
        }

        return outputConveyor
    }
}
