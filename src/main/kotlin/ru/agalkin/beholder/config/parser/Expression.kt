package ru.agalkin.beholder.config.parser

/**
 * Выражение (команда) из токенов
 *
 * command arg arg;
 * или
 * command arg arg { child; child }
 */
class Expression {
    private val arguments = ArrayList<ArgumentToken>()
    private val childExpressions = ArrayList<Expression>()

    companion object {
        fun fromTokens(tokens: List<Token>): Expression {
            val root = Expression()
            val tokenIterator = tokens.listIterator()
            root.importChildExpressions(tokenIterator)
            if (tokenIterator.hasNext()) {
                // не все токены распихались по выражениям
                throw ParseException("Unexpected leftover tokens:", tokenIterator)
            }
            return root
        }
    }

    private fun importChildExpressions(tokens: ListIterator<Token>) {
        // Общая логика такая: пытаемся присобачить токены в child, пока влезают.
        // Сначала он получает аргументы, а потом возможно ещё и детей.
        // Если мы нашли терминатор (; либо закрылся {блок детей}), то child кладём
        // в текущее выражение и рекурсия (ищем следующих детей текущего выражения).
        val child = Expression()
        while (tokens.hasNext()) {
            val token = tokens.next()

            // нащупали ; = детей нет, выражение кончилось
            if (token is SemicolonToken) {
                if (!child.arguments.isEmpty()) {
                    childExpressions.add(child)
                }
                return importChildExpressions(tokens)
            }

            // нащупали открывашку = поехал блок и потом выражение кончилось
            if (token is OpenBraceToken) {
                if (child.arguments.isEmpty()) {
                    tokens.previous()
                    throw ParseException("Cannot have a command without command name:", tokens)
                }
                child.importChildExpressions(tokens)
                if (!tokens.hasNext() || tokens.next() !is CloseBraceToken) {
                    tokens.previous()
                    throw ParseException("Invalid closing brace placement:", tokens)
                }
                childExpressions.add(child)
                return importChildExpressions(tokens)
            }

            // нащупали закрывашку = кончились дети
            if (token is CloseBraceToken) {
                if (!child.arguments.isEmpty()) {
                    childExpressions.add(child)
                }
                // возвращаем позицию обратно, чтобы снаружи в родителе убедиться в необходимости закрывашки
                // (у рута нет ни открывашек, ни закрывашек)
                tokens.previous()
                return
            }

            // специальные типы токенов обработаны, теперь более прозаичные
            if (token is ArgumentToken) {
                if (token !is LiteralToken && child.arguments.isEmpty()) {
                    tokens.previous()
                    throw ParseException("Commands must start with a literal:", tokens)
                }
                child.arguments.add(token)
                continue
            }

            tokens.previous()
            throw ParseException("Cannot parse token ${token.javaClass.simpleName}:", tokens)
        }
    }

    fun getChildrenDefinition(indent: String = "")
        = listToString(childExpressions, { it.getDefinition(indent) })

    private fun getDefinition(indent: String = ""): String {
        val sb = StringBuilder()

        // args
        sb.append(indent).append(arguments.joinToString(" ") { it.getDefinition() })

        // child expressions
        if (childExpressions.isEmpty()) {
            sb.append(";\n")
        } else {
            sb.append(" {\n")
            sb.append(getChildrenDefinition(indent + "    "))
            sb.append(indent).append("}\n")
        }

        return sb.toString()
    }
}
