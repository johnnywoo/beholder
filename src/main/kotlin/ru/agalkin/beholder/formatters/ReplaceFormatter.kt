package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import java.util.regex.Pattern

class ReplaceFormatter(private val regexp: Pattern, replacement: String, subject: String) : Formatter {
    private val replacementFormatter = InterpolateStringFormatter(replacement)
    private val subjectFormatter = InterpolateStringFormatter(subject)

    override fun formatMessage(message: Message): String {
        val interpolatedReplacement = replacementFormatter.formatMessage(message)
        val interpolatedSubject = subjectFormatter.formatMessage(message)
        try {
            return regexp.matcher(interpolatedSubject).replaceAll(interpolatedReplacement) ?: interpolatedSubject
        } catch (e: Throwable) {
            // не получилось по каким-то причинам совершить замену
            // скорее всего ошибка в строке замены
            InternalLog.exception(e)
            return interpolatedSubject
        }
    }
}
