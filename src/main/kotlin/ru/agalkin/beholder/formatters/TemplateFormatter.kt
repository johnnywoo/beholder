package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import java.util.regex.Pattern

abstract class TemplateFormatter : Formatter {
    companion object {
        private val regexp = Pattern.compile("(?>\\{\\$([a-z][a-z0-9_]*)}|\\$([a-z][a-z0-9_]*))", Pattern.CASE_INSENSITIVE)

        fun create(template: String): TemplateFormatter {
            // template does not contain any fields
            if (!hasTemplates(template)) {
                return NoVariablesFormatter(template)
            }
            // whole template is $fieldName
            val matcher = regexp.matcher(template)
            if (matcher.matches()) {
                return SingleFieldFormatter(matcher.group(1) ?: matcher.group(2))
            }

            return InterpolateStringFormatter(template)
        }

        val payloadFormatter = create("\$payload")

        fun hasTemplates(string: String)
            = regexp.matcher(string).find()
    }

    private class NoVariablesFormatter(template: String) : TemplateFormatter() {
        private val fieldValue = FieldValue.fromString(template)
        override fun formatMessage(message: Message)
            = fieldValue
    }

    private class SingleFieldFormatter(private val field: String) : TemplateFormatter() {
        override fun formatMessage(message: Message)
            = message.getFieldValue(field)
    }

    private class InterpolateStringFormatter(private val template: String) : TemplateFormatter() {
        override fun formatMessage(message: Message): FieldValue {
            return FieldValue.fromString(
                regexp.matcher(template).replaceAll({
                    message.getStringField(it.group(1) ?: it.group(2))
                })
            )
        }
    }
}
