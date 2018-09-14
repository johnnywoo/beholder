package ru.agalkin.beholder.formatters

import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.ParseException

abstract class TemplateFormatter : Formatter {
    companion object {
        fun create(template: String): TemplateFormatter {
            val parsed = TemplateParser.parse(template, true)

            if (parsed.size == 1) {
                // template does not contain any fields
                return NoVariablesFormatter(parsed.first())
            }

            // whole template is $fieldName
            if (parsed.size == 2 && parsed[0].isEmpty()) {
                return SingleFieldFormatter(parsed[1])
            }

            return InterpolateStringFormatter(parsed)
        }

        val payloadFormatter: TemplateFormatter = PayloadFormatter()

        fun hasTemplates(string: String)
            = TemplateParser.parse(string, true).size > 1
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

    private class InterpolateStringFormatter(parsedTemplate: List<String>) : TemplateFormatter() {
        private val parsedArray = parsedTemplate.toTypedArray()

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

    object TemplateParser {
        fun parse(template: String, ignoreInvalidSyntaxIfPossible: Boolean): List<String> {
            var state = State.TEXT
            val chunks = mutableListOf("")
            var currentCharIndex = 0
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
                            '{' -> {
                                chunks.add("")
                                state = State.OPEN_BRACE
                            }
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
                                if (ignoreInvalidSyntaxIfPossible) {
                                    chunks.removeAt(chunks.indices.last)
                                    chunks[chunks.indices.last] = chunks[chunks.indices.last] + '$' + char
                                } else {
                                    throw ParseException("Char '$char' (offset $currentCharIndex) is illegal as field name start: $template")
                                }
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
                    State.OPEN_BRACE -> {
                        when (char) {
                            '$' -> {
                                state = State.BRACED_FIELD_START
                            }
                            else -> {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + '{' + char
                            }
                        }
                    }
                    State.BRACED_FIELD_START -> {
                        when (char) {
                            in 'a'..'z', in 'A'..'Z', '_' -> {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                                state = State.BRACED_FIELD
                            }
                            else -> {
                                // после доллара сразу нелепая фигня
                                if (ignoreInvalidSyntaxIfPossible) {
                                    chunks.removeAt(chunks.indices.last)
                                    chunks[chunks.indices.last] = chunks[chunks.indices.last] + '{' + '$' + char
                                } else {
                                    throw ParseException("Char '$char' (offset $currentCharIndex) is illegal as field name start: $template")
                                }
                            }
                        }
                    }
                    State.BRACED_FIELD -> {
                        when (char) {
                            in 'a'..'z', in 'A'..'Z', in '0'..'9', '_' -> {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                            }
                            '}' -> {
                                // кончилось название поля
                                chunks.add("")
                                state = State.TEXT
                            }
                            else -> {
                                // фигурные скобки ещё не закрылись, а символ не годится для имени поля
                                throw ParseException("Char '$char' (offset $currentCharIndex) is illegal in field names: $template")
                            }
                        }
                    }
                }
                currentCharIndex++
            }

            if (chunks.last().isEmpty() && chunks.size > 1) {
                chunks.removeAt(chunks.indices.last)
            }

            return chunks
        }

        enum class State {
            TEXT,
            UNBRACED_FIELD_START,
            UNBRACED_FIELD,
            OPEN_BRACE,
            BRACED_FIELD_START,
            BRACED_FIELD,
            ESCAPE
        }
    }
}
