package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.*

class FlowCommand(arguments: List<ArgumentToken>) : CommandAbstract(arguments) {
    override fun createSubcommand(args: List<ArgumentToken>): CommandAbstract?
        = when (args[0].getValue()) {
            "from"    -> FromCommand(args)
            "convert" -> ConvertCommand(args)
            "to"      -> ToCommand(args)
            else      -> null
        }

    override fun start() {
        // внутри flow команды по очереди обрабатывают сообщения

        if (subcommands.size < 2) {
            // flow бесполезен без хотя бы двух команд
            return
        }

        for (i in subcommands.indices) {
            if (!subcommands.indices.contains(i + 1)) {
                continue
            }
            val prevCommand = subcommands[i]
            val nextCommand = subcommands[i + 1]

            // сообщение из первой команды пихаем во вторую, и т.д.
            prevCommand.addReceiver { nextCommand.emit(it) }
        }

        super.start()
    }
}
