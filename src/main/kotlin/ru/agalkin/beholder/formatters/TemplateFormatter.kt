package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.Message
import java.util.regex.Pattern

abstract class TemplateFormatter : Formatter {
    companion object {
        private val regexp = Pattern.compile("\\$([a-z][a-z0-9_]*)", Pattern.CASE_INSENSITIVE)

        fun create(template: String): Formatter {
            // template does not contain any fields
            if (!regexp.matcher(template).find()) {
                return NoVariablesFormatter(template)
            }
            // whole template is $fieldName
            if (regexp.matcher(template).matches()) {
                return SingleFieldFormatter(template.substring(1))
            }

            return InterpolateStringFormatter(template)
        }
    }

    private class NoVariablesFormatter(private val template: String) : TemplateFormatter() {
        override fun formatMessage(message: Message)
            = template
    }

    private class SingleFieldFormatter(private val field: String) : TemplateFormatter() {
        override fun formatMessage(message: Message)
            = message.getStringField(field)
    }

    private class InterpolateStringFormatter(private val template: String) : TemplateFormatter() {
        override fun formatMessage(message: Message): String
            = regexp.matcher(template).replaceAll({ message[it.group(1)] ?: "" })
    }
}
