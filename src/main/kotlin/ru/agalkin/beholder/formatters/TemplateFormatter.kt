package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ParseException
import java.util.regex.Matcher
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

        val payloadFormatter: TemplateFormatter = PayloadFormatter()

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

    private class PayloadFormatter : TemplateFormatter() {
        override fun formatMessage(message: Message)
            = message.getPayloadValue()
    }

    private class InterpolateStringFormatter(private val template: String) : TemplateFormatter() {
        override fun formatMessage(message: Message): FieldValue {
            return FieldValue.fromString(
                regexp.matcher(template).replaceAll({
                    Matcher.quoteReplacement(
                        message.getStringField(it.group(1) ?: it.group(2))
                    )
                })
            )
        }
    }

    object TemplateParser {
        fun parse(template: String): List<String> {
            var state = State.TEXT
            val chunks = mutableListOf<String>("")
            var i = 0
            for (char in template) {
                when (state) {
                    State.TEXT -> {
                        when (char) {
                            '$' -> {
                                chunks.add("")
                                state = State.UNBRACED_FIELD_START
                            }
                            '\\' -> {
                                state = State.ESCAPE
                            }
//                            '{' -> {
//                                chunks.add("")
//                                state = State.OPEN_BRACE
//                            }
                            else -> {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                            }
                        }
                    }
                    State.ESCAPE -> {
                        chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                        state = State.TEXT
                    }
                    State.UNBRACED_FIELD_START -> {
                        when (char) {
                            in 'a'..'z', in 'A'..'Z', '_' -> {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                                state = State.UNBRACED_FIELD
                            }
                            else -> {
                                // после доллара сразу нелепая фигня
                                throw ParseException("Char '$char' (offset $i) is illegal as field name start: $template")
                            }
                        }
                    }
                    State.UNBRACED_FIELD -> {
                        when (char) {
                            in 'a'..'z', in 'A'..'Z', in '0'..'9', '_' -> {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                            }
                            else -> {
                                // кончилось название поля
                                chunks.add(char.toString())
                                state = State.TEXT
                            }
                        }
                    }
//                    State.OPEN_BRACE -> {
//
//                    }
                }
                i++
            }
            return chunks
        }

        enum class State {
            TEXT,
            UNBRACED_FIELD_START,
            UNBRACED_FIELD,
//            OPEN_BRACE,
            ESCAPE
        }
    }
}
