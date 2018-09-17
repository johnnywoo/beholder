package ru.agalkin.beholder.config

import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.QuotedStringToken

object TemplateParser {
    fun hasTemplates(token: ArgumentToken): Boolean {
        val result = parseToken(token, false)
        if (result == null) {
            return false
        }
        return result.parts.size > 1
    }

    fun parseToken(token: ArgumentToken, ignoreInvalidSyntax: Boolean): Result? {
        when (token) {
            is LiteralToken -> {
                return parse(token.getDefinition(), ignoreInvalidSyntax, false)
            }
            is QuotedStringToken -> {
                val definition = token.getDefinition()
                return parse(definition.substring(1, definition.length - 1), ignoreInvalidSyntax, true)
            }
        }
        return null
    }

    fun parse(template: String, ignoreInvalidSyntaxIfPossible: Boolean, allowEscaping: Boolean): Result {
        var state = State.TEXT
        val chunks = mutableListOf("")
        var currentCharIndex = 0
        for (char in template) {
            when (state) {
                State.TEXT -> {
                    when (char) {
                        '$' -> {
                            state = State.UNBRACED_FIELD_START
                        }
                        '\\' -> {
                            if (allowEscaping) {
                                state = State.ESCAPE
                            } else {
                                chunks[chunks.indices.last] = chunks[chunks.indices.last] + char
                            }
                        }
                        '{' -> {
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
                            chunks.add(char.toString())
                            state = State.UNBRACED_FIELD
                        }
                        else -> {
                            // после доллара сразу нелепая фигня
                            if (ignoreInvalidSyntaxIfPossible) {
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
                            state = State.TEXT
                        }
                    }
                }
                State.BRACED_FIELD_START -> {
                    when (char) {
                        in 'a'..'z', in 'A'..'Z', '_' -> {
                            chunks.add(char.toString())
                            state = State.BRACED_FIELD
                        }
                        else -> {
                            // после доллара сразу нелепая фигня
                            if (ignoreInvalidSyntaxIfPossible) {
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

        return Result(chunks)
    }

    class Result(val parts: List<String>)

    private enum class State {
        TEXT,
        UNBRACED_FIELD_START,
        UNBRACED_FIELD,
        OPEN_BRACE,
        BRACED_FIELD_START,
        BRACED_FIELD,
        ESCAPE
    }
}
