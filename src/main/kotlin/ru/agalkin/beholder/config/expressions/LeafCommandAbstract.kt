package ru.agalkin.beholder.config.expressions

import ru.agalkin.beholder.Beholder

abstract class LeafCommandAbstract(app: Beholder, arguments: Arguments) : CommandAbstract(app, arguments) {
    final override fun createSubcommand(args: Arguments): CommandAbstract?
        = null
}
