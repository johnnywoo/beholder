package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken

class CommandArguments(commandNameToken: LiteralToken) : Arguments {
    private val args = mutableListOf<ArgumentToken>(commandNameToken)

    private var index = 0

    fun add(argumentToken: ArgumentToken)
        = args.add(argumentToken)

    override fun getCommandName()
        = args[0].getValue()

    override fun toList(): List<ArgumentToken>
        = args.toList()

    override fun shiftToken(errorMessage: String): ArgumentToken {
        if (args.indices.contains(index + 1)) {
            index++
            return args[index]
        }
        throw CommandException(errorMessage)
    }

    override fun end() {
        if (args.indices.contains(index + 1)) {
            throw CommandException("Too many arguments for `${getCommandName()}`")
        }
    }
}
