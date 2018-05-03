package ru.agalkin.beholder.commands

import ru.agalkin.beholder.config.expressions.Arguments

class SwitchDefaultCommand(arguments: Arguments) : SwitchBranchCommandAbstract(arguments) {
    init {
        arguments.end()
    }
}
