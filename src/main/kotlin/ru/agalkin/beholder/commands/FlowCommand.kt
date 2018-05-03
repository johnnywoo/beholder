package ru.agalkin.beholder.commands

import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.*

open class FlowCommand(arguments: Arguments) : CommandAbstract(arguments) {
    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "flow"   -> FlowCommand(args)
            "from"   -> FromCommand(args)
            "switch" -> SwitchCommand(args)
            "parse"  -> ParseCommand(args)
            "set"    -> SetCommand(args)
            "keep"   -> KeepCommand(args)
            "to"     -> ToCommand(args)
            else     -> null
        }

    private val isOpenAtStart: Boolean
    private val isOpenAtEnd: Boolean

    init {
        when (arguments.shiftLiteralOrNull("out", "closed")) {
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

    override fun input(message: Message) {
        if (isOpenAtStart && !subcommands.isEmpty()) {
            // вход flow направляем в первую вложенную команду
            val firstCommand = subcommands[0]
            firstCommand.input(message)
        }

        // также flow копирует все входящие сообщения на выход
        output.sendMessageToSubscribers(message)
    }

    override fun start() {
        if (isOpenAtStart && isOpenAtEnd) {
            // если наш flow открыт с обеих сторон, роутер заклинит в бесконечном цикле
            // мы так не хотим
            throw BeholderException("Invalid flow configuration: isOpenAtStart and isOpenAtEnd cannot be both enabled")
        }

        setupConveyor(isOpenAtEnd)

        super.start()
    }
}
