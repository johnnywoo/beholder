package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import java.util.regex.Pattern

class ReplaceFormatter(
    private val regexp: Pattern,
    private val replacementTemplate: TemplateFormatter,
    private val subjectTemplate: TemplateFormatter
) : Formatter {
    override fun formatMessage(message: Message): String {
        val replacement = replacementTemplate.formatMessage(message)
        val subject     = subjectTemplate.formatMessage(message)
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
