package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import java.util.regex.Pattern

class ReplaceFormatter(private val regexp: Pattern, replacementTemplate: String, subjectTemplate: String) : Formatter {
    private val replacementFormatter = TemplateFormatter.create(replacementTemplate)
    private val subjectFormatter     = TemplateFormatter.create(subjectTemplate)

    override fun formatMessage(message: Message): String {
        val replacement = replacementFormatter.formatMessage(message)
        val subject     = subjectFormatter.formatMessage(message)
        try {
            return regexp.matcher(subject).replaceAll(replacement) ?: subject
        } catch (e: Throwable) {
            // не получилось по каким-то причинам совершить замену
            // скорее всего ошибка в строке замены
            InternalLog.exception(e)
            return subject
        }
    }
}
