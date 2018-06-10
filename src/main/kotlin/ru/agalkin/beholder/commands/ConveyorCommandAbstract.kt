package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract

abstract class ConveyorCommandAbstract(
    app: Beholder,
    arguments: Arguments,
    private val sendInputToOutput: Boolean,
    private val sendInputToSubcommands: Boolean,
    private val sendLastSubcommandToOutput: Boolean
) : CommandAbstract(app, arguments) {

    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "drop"   -> DropCommand(app, args)
            "flow"   -> FlowCommand(app, args)
            "from"   -> FromCommand(app, args)
            "join"   -> JoinCommand(app, args)
            "keep"   -> KeepCommand(app, args)
            "parse"  -> ParseCommand(app, args)
            "set"    -> SetCommand(app, args)
            "switch" -> SwitchCommand(app, args)
            "tee"    -> TeeCommand(app, args)
            "to"     -> ToCommand(app, args)
            else     -> null
        }


    override fun input(message: Message) {
        // сообщение едет по конвейеру от входа через субкоманды

        if (sendInputToSubcommands) {
            if (subcommands.isEmpty()) {
                // Требуется отправить сообщение в субкоманды, однако субкоманд нет ни одной.
                // В таком случае считаем, что сообщение успешно пролетело сквозь все субкоманды и пытается выбраться из последней.
                if (sendLastSubcommandToOutput) {
                    output.sendMessageToSubscribers(if (sendInputToOutput) message.copy() else message)
                }
            } else {
                subcommands[0].input(if (sendInputToOutput) message.copy() else message)
            }
        }

        if (sendInputToOutput) {
            // Входящее сообщение идёт на выход.
            // Оригинальным сообщением (не копией) можно пользоваться только в самом конце!
            output.sendMessageToSubscribers(message)
        }
    }

    override fun start() {
        if (subcommands.isEmpty()) {
            return
        }

        // соединяем команды в конвейер
        for ((prevCommand, nextCommand) in subcommands.zipWithNext()) {
            // сообщение из первой команды пихаем во вторую, и т.д.
            prevCommand.output.addSubscriber {
                nextCommand.input(it)
            }
        }

        if (sendLastSubcommandToOutput) {
            // выход последней субкоманды направляем на выход из текущей команды
            val lastCommand = subcommands.last()
            lastCommand.output.addSubscriber {
                output.sendMessageToSubscribers(it)
            }
        }

        super.start()
    }
}
