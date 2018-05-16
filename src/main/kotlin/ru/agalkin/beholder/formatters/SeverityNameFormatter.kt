package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message

class SeverityNameFormatter(
    private val template: TemplateFormatter,
    private val isLowerCase: Boolean
) : Formatter {
    private val names = mapOf(
        "0" to FieldValue.fromString("EMERG"),
        "1" to FieldValue.fromString("ALERT"),
        "2" to FieldValue.fromString("CRIT"),
        "3" to FieldValue.fromString("ERR"),
        "4" to FieldValue.fromString("WARNING"),
        "5" to FieldValue.fromString("NOTICE"),
        "6" to FieldValue.fromString("INFO"),
        "7" to FieldValue.fromString("DEBUG")
    )

    private val lowerNames = mapOf(
        "0" to FieldValue.fromString("emerg"),
        "1" to FieldValue.fromString("alert"),
        "2" to FieldValue.fromString("crit"),
        "3" to FieldValue.fromString("err"),
        "4" to FieldValue.fromString("warning"),
        "5" to FieldValue.fromString("notice"),
        "6" to FieldValue.fromString("info"),
        "7" to FieldValue.fromString("debug")
    )

    override fun formatMessage(message: Message): FieldValue {
        val numberValue = template.formatMessage(message)
        return (if (isLowerCase) lowerNames else names).getOrDefault(numberValue.toString(), numberValue)
    }
}
