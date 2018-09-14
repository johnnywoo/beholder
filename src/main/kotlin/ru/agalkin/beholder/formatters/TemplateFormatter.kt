package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.TemplateParser

abstract class TemplateFormatter : Formatter {
    companion object {
        fun create(parseResult: TemplateParser.Result): TemplateFormatter {
            if (parseResult.parts.size == 1) {
                // template does not contain any fields
                return NoVariablesFormatter(parseResult.parts.first())
            }

            // whole template is $fieldName
            if (parseResult.parts.size == 2 && parseResult.parts[0].isEmpty()) {
                return SingleFieldFormatter(parseResult.parts[1])
            }

            return InterpolateStringFormatter(parseResult)
        }

        fun ofField(fieldName: String): TemplateFormatter
            = SingleFieldFormatter(fieldName)

        val payloadFormatter: TemplateFormatter = PayloadFormatter()
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

    private class PayloadFormatter : TemplateFormatter() {
        override fun formatMessage(message: Message)
            = message.getPayloadValue()
    }

    private class InterpolateStringFormatter(parsedTemplate: TemplateParser.Result) : TemplateFormatter() {
        private val parsedArray = parsedTemplate.parts.toTypedArray()

        override fun formatMessage(message: Message): FieldValue {
            val sb = StringBuilder(parsedArray[0])
            var i = 1
            val size = parsedArray.size
            while (i < size) {
                sb.append(message.getStringField(parsedArray[i]))
                if (i + 1 < size) {
                    sb.append(parsedArray[i + 1])
                }
                i += 2
            }
            return FieldValue.fromString(sb.toString())
        }
    }
}
