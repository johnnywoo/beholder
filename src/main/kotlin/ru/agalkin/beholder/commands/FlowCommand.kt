package ru.agalkin.beholder.commands

import ru.agalkin.beholder.config.expressions.*

open class FlowCommand(arguments: Arguments) : CommandAbstract(arguments) {
    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "flow"  -> FlowCommand(args)
            "from"  -> FromCommand(args)
            "parse" -> ParseCommand(args)
            "set"   -> SetCommand(args)
            "to"    -> ToCommand(args)
            else    -> null
        }

    init {
        arguments.end()
    }

    override fun start() {
        // внутри flow команды по очереди обрабатывают сообщения

        // вход flow направляем в первую вложенную команду
        // также он будет проброшен на выход (для этого наш текущий flow должен находиться внутри другого flow или рута)
        val firstCommand = subcommands.getOrNull(0)
        if (firstCommand != null) {
            router.addSubscriber({ firstCommand.receiveMessage(it) })
        }

        // если команда одна, то соединять будет нечего
        if (subcommands.size < 2) {
            return
        }

        // соединяем команды в конвейер
        for (i in subcommands.indices) {
            if (!subcommands.indices.contains(i + 1)) {
                continue
            }
            val prevCommand = subcommands[i]
            val nextCommand = subcommands[i + 1]

            // сообщение из первой команды пихаем во вторую, и т.д.
            prevCommand.router.addSubscriber({ nextCommand.receiveMessage(it) })
        }

        super.start()
    }
}
