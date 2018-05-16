package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import java.util.regex.Pattern

class ReplaceFormatter(
    private val regexp: Pattern,
    private val replacementTemplate: TemplateFormatter,
    private val subjectTemplate: TemplateFormatter
) : Formatter {
    override fun formatMessage(message: Message): FieldValue {
        val replacementValue = replacementTemplate.formatMessage(message)
        val subjectValue     = subjectTemplate.formatMessage(message)
        try {
            return FieldValue.fromString(
                regexp.matcher(subjectValue.toString()).replaceAll(replacementValue.toString())
            )
        } catch (e: Throwable) {
            // не получилось по каким-то причинам совершить замену
            // скорее всего ошибка в строке замены
            InternalLog.exception(e)
            return subjectValue
        }
    }
}
