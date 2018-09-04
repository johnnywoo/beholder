package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract

abstract class ConveyorCommandAbstract(app: Beholder, arguments: Arguments) : CommandAbstract(app, arguments) {

    override fun createSubcommand(args: Arguments): CommandAbstract?
        = when (args.getCommandName()) {
            "drop"   -> DropCommand(app, args)
            "flow"   -> FlowCommand(app, args)
            "from"   -> FromCommand(app, args)
            "join"   -> JoinCommand(app, args)
            "keep"   -> KeepCommand(app, args)
            "parse"  -> ParseCommand(app, args)
            "set"    -> SetCommand(app, args)
            "switch" -> SwitchCommand(app, args)
            "tee"    -> TeeCommand(app, args)
            "to"     -> ToCommand(app, args)
            else     -> null
        }
}
