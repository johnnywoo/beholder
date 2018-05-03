package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract

abstract class SwitchBranchCommandAbstract(arguments: Arguments) : CommandAbstract(arguments) {
    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "flow"   -> SwitchCommand(args)
            "from"   -> FromCommand(args)
            "switch" -> SwitchCommand(args)
            "parse"  -> ParseCommand(args)
            "set"    -> SetCommand(args)
            "keep"   -> KeepCommand(args)
            "to"     -> ToCommand(args)
            else     -> null
        }

    override fun input(message: Message) {
        // внутри case/default сообщение едет по конвейеру от входа через субкоманды

        if (subcommands.isEmpty()) {
            // вложенных команд нет = просто копируем сообщения на выход
            output.sendMessageToSubscribers(message)
            return
        }

        // вход case / default направляем в первую вложенную команду
        val firstCommand = subcommands[0]
        firstCommand.input(message)
    }

    override fun start() {
        // внутри case/default сообщение едет по конвейеру от входа через субкоманды и на выход
        setupConveyor(true)

        super.start()
    }
}
