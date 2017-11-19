package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.config.parser.ArgumentToken

abstract class LeafCommandAbstract(arguments: List<ArgumentToken>) : CommandAbstract(arguments) {
    override fun createSubcommand(args: List<ArgumentToken>): CommandAbstract?
        = null
}
