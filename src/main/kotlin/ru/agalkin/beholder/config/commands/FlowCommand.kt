package ru.agalkin.beholder.config.commands

open class FlowCommand(arguments: Arguments) : CommandAbstract(arguments) {
    companion object {
        val help = """
            |flow {subcommands}
            |
            |Subcommands: `flow`, `from`, `parse`, `set`, `to`.
            |
            |Use this command to create separate flows of messages.
            |Rule of thumb is: what happens in `flow` stays in `flow`.
            |
            |Message routing:
            |Incoming message: a copy goes into first subcommand, a copy is emitted out of the `flow`.
            |Exit of last subcommand: messages are discarded.
            |
            |Example config with a caveat:
            |  from udp 1001;
            |  to tcp 1.2.3.4:1002; # here we send messages from port 1001
            |  from udp 1003;
            |  to tcp 1.2.3.4:1004; # receives messages from both ports 1001 AND 1003!
            |
            |This config will create two separate flows of messages:
            |  flow {from udp 1001; to tcp 1.2.3.4:1002}
            |  flow {from udp 1003; to tcp 1.2.3.4:1004}
            |""".trimMargin()
    }

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
            prevCommand.router.subscribers.add({ nextCommand.emit(it) })
        }

        super.start()
    }
}
