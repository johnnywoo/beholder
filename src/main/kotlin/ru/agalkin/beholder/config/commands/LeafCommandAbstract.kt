package ru.agalkin.beholder.config.commands

abstract class LeafCommandAbstract(arguments: Arguments) : CommandAbstract(arguments) {
    override fun createSubcommand(args: Arguments): CommandAbstract?
        = null
}
