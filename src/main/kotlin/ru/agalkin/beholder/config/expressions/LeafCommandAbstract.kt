package ru.agalkin.beholder.config.expressions

abstract class LeafCommandAbstract(arguments: Arguments) : CommandAbstract(arguments) {
    final override fun createSubcommand(args: Arguments): CommandAbstract?
        = null
}
