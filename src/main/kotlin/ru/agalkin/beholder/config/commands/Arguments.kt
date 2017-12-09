package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken

open class Arguments(commandNameToken: LiteralToken) {
    protected val args = mutableListOf<ArgumentToken>(commandNameToken)

    private var index = 0

    fun add(argumentToken: ArgumentToken)
        = args.add(argumentToken)

    open fun getCommandName()
        = args[0].getValue()

    fun toList(): List<ArgumentToken>
        = args.toList()

    fun shiftToken(errorMessage: String): ArgumentToken {
        if (args.indices.contains(index + 1)) {
            index++
            return args[index]
        }
        throw CommandException(errorMessage)
    }

    fun shift(errorMessage: String)
        = shiftToken(errorMessage).getValue()

    fun shiftFieldName(errorMessage: String): String {
        val arg = shiftToken(errorMessage)
        if (arg !is LiteralToken || !arg.getValue().startsWith('$')) {
            throw CommandException(errorMessage)
        }
        return arg.getValue().substring(1)
    }

    fun end() {
        if (args.indices.contains(index + 1)) {
            throw CommandException("Too many arguments for `${getCommandName()}`")
        }
    }

    // сомнительная хрень, тут надо что-то перепридумать
    object RootArguments : Arguments(LiteralToken(' ')) {
        init {
            args.clear()
        }
        override fun getCommandName()
            = "<root>"
    }
}
