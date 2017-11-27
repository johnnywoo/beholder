package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken

class Arguments(private val args: List<ArgumentToken>) {
    private var index = 0

    fun getCommandName()
        = args[0].getValue()

    fun toList(): List<ArgumentToken>
        = args

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
            throw CommandException("Too many arguments for `${args[0]}`")
        }
    }
}

