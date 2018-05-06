package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message

class SeverityNameFormatter(
    private val template: TemplateFormatter,
    private val isLowerCase: Boolean
) : Formatter {
    private val names = mapOf(
        "0" to "EMERG",
        "1" to "ALERT",
        "2" to "CRIT",
        "3" to "ERR",
        "4" to "WARNING",
        "5" to "NOTICE",
        "6" to "INFO",
        "7" to "DEBUG"
    )

    private val lowerNames = mapOf(
        "0" to "emerg",
        "1" to "alert",
        "2" to "crit",
        "3" to "err",
        "4" to "warning",
        "5" to "notice",
        "6" to "info",
        "7" to "debug"
    )

    override fun formatMessage(message: Message): String {
        val number = template.formatMessage(message)
        return (if (isLowerCase) lowerNames else names).getOrDefault(number, number)
    }
}
