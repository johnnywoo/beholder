package ru.agalkin.beholder.commands

import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.config.expressions.*

open class FlowCommand(arguments: Arguments) : CommandAbstract(arguments) {
    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "flow"  -> FlowCommand(args)
            "from"  -> FromCommand(args)
            "parse" -> ParseCommand(args)
            "set"   -> SetCommand(args)
            "keep"  -> KeepCommand(args)
            "to"    -> ToCommand(args)
            else    -> null
        }

    private val isOpenAtStart: Boolean
    private val isOpenAtEnd: Boolean

    init {
        when (arguments.shiftLiteralOrNull(setOf("out", "closed"))) {
            "out" -> {
                isOpenAtStart = false
                isOpenAtEnd   = true
            }
            "closed" -> {
                isOpenAtStart = false
                isOpenAtEnd   = false
            }
            else -> {
                isOpenAtStart = true
                isOpenAtEnd   = false
            }
        }

        arguments.end()
    }

    override fun start() {
        if (isOpenAtStart && isOpenAtEnd) {
            // если наш flow открыт с обеих сторон, роутер заклинит в бесконечном цикле
            // мы так не хотим
            throw BeholderException("Invalid flow configuration: isOpenAtStart and isOpenAtEnd cannot be both enabled")
        }

        // стандартный режим flow (не out и не closed)
        if (isOpenAtStart && !subcommands.isEmpty()) {
            // вход flow направляем в первую вложенную команду
            val firstCommand = subcommands[0]
            router.addSubscriber({ firstCommand.receiveMessage(it) })
        }

        // внутри flow команды по очереди обрабатывают сообщения
        // если команда одна, то соединять будет нечего
        if (subcommands.size >= 2) {
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
        }

        // режим flow out (не стандартный и не closed)
        if (isOpenAtEnd && !subcommands.isEmpty()) {
            // выход последней команды направляем на выход из flow
            val lastCommand = subcommands.last()
            lastCommand.router.addSubscriber { receiveMessage(it) }
        }

        super.start()
    }
}
