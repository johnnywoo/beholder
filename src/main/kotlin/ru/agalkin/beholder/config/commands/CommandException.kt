package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.BeholderException

class CommandException(message: String) : BeholderException(message) {
    constructor(e: BeholderException): this(e.message!!)
}
