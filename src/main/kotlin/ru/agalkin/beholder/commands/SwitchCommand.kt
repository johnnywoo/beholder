package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.CommandException

class SwitchCommand(arguments: Arguments) : CommandAbstract(arguments) {
    private val template = arguments.shiftStringTemplate("`switch` needs an argument")
    init {
        arguments.end()
    }

    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "case"    -> if (!hasDefaultBlock()) SwitchCaseCommand(args, template) else throw CommandException("`switch` cannot have `case` subcommand after `default`")
            "default" -> if (!hasDefaultBlock()) SwitchDefaultCommand(args) else throw CommandException("`switch` cannot have multiple `default` subcommands")
            else      -> null
        }

    private fun hasDefaultBlock()
        = subcommands.any { it is SwitchDefaultCommand }

    override fun start() {
        // выход всех вложенных команд (case / default) направляем на выход из switch
        for (subcommand in subcommands) {
            subcommand.output.addSubscriber { output.sendMessageToSubscribers(it) }
        }

        super.start()
    }

    interface SwitchSubcommand {
        fun inputIfMatches(message: Message): Boolean
    }

    override fun input(message: Message) {
        // если входящее сообщение подходит под условие вложенной команды, направляем его в неё
        for (subcommand in subcommands) {
            if (subcommand is SwitchSubcommand && subcommand.inputIfMatches(message)) {
                break
            }
        }
    }
}
