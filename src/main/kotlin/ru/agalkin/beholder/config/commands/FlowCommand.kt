package ru.agalkin.beholder.config.commands

class FlowCommand(arguments: Arguments) : CommandAbstract(arguments) {
    companion object {
        val help = """
            |flow {subcommands}
            |
            |Subcommands: `from`, `parse`, `set`, `to`.
            |
            |The first level of config can only contain `flow` commands.
            |`flow` creates a flow of messages, which are routed consecutively through its subcommands.
            |
            |Example config with a caveat:
            |  flow {
            |     from udp 1001;
            |     to tcp 1.2.3.4:1002; # here we send messages from port 1001
            |     from udp 1003;
            |     to tcp 1.2.3.4:1004; # receives messages from both ports 1001 AND 1003!
            |  }
            |
            |This config will create two separate flows of messages:
            |  flow {from udp 1001; to tcp 1.2.3.4:1002}
            |  flow {from udp 1003; to tcp 1.2.3.4:1004}
            |""".trimMargin()
    }

    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
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
            prevCommand.receivers.add({ nextCommand.emit(it) })
        }

        super.start()
    }
}
