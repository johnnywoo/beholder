package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Conveyor
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.formatters.TemplateFormatter
import ru.agalkin.beholder.inflaters.RegexpInflater
import java.util.regex.Pattern

class SwitchCaseCommand(
    app: Beholder,
    arguments: Arguments,
    private val template: TemplateFormatter
) : ConveyorCommandAbstract(app, arguments), SwitchCommand.SwitchSubcommand {
    private val matcher: Matcher
    init {
        val regexp = arguments.shiftRegexpOrNull()
        if (regexp != null) {
            matcher = RegexpMatcher(regexp)
        } else {
            matcher = ExactMatcher(arguments.shiftStringTemplate("`case` needs a regexp or a string"))
        }
        arguments.end()
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        var currentConveyor = conveyor
        for (subcommand in subcommands) {
            currentConveyor = subcommand.buildConveyor(currentConveyor)
        }

        return currentConveyor
    }

    override fun getConditionStep(): Conveyor.Step {
        return matcher
    }

    private abstract inner class Matcher : Conveyor.Step {
        override fun getDescription()
            = getDefinition(includeSubcommands = false)
    }

    private inner class RegexpMatcher(regexp: Pattern) : Matcher() {
        private val regexpInflater = RegexpInflater(regexp, template)

        override fun execute(message: Message): Conveyor.StepResult {
            if (!regexpInflater.inflateMessageFieldsInplace(message)) {
                return Conveyor.StepResult.DROP
            }
            return Conveyor.StepResult.CONTINUE
        }
    }

    private inner class ExactMatcher(private val caseTemplate: TemplateFormatter) : Matcher() {
        override fun execute(message: Message): Conveyor.StepResult {
            if (caseTemplate.formatMessage(message).toString() != template.formatMessage(message).toString()) {
                return Conveyor.StepResult.DROP
            }
            return Conveyor.StepResult.CONTINUE
        }
    }
}
