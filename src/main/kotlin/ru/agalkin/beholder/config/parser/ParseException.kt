package ru.agalkin.beholder.config.parser

import ru.agalkin.beholder.BeholderException

class ParseException(message: String) : BeholderException(message) {
    companion object {
        fun fromIterator(message: String, tokens: ListIterator<Token>, rewind: Int = 0): ParseException {
            rewindIterator(tokens, rewind)

            val sb = StringBuilder(message)
            while (tokens.hasNext()) {
                val token = tokens.next()
                if (token !is SemicolonToken) {
                    sb.append(" ")
                }
                sb.append(token.getDefinition())
            }
            return ParseException(sb.toString())
        }

        private tailrec fun <T> rewindIterator(iterator: ListIterator<T>, offset: Int) {
            if (offset > 0) {
                iterator.previous()
                rewindIterator(iterator, offset - 1)
            }
        }

        fun fromList(message: String, tokens: List<*>): ParseException {
            val sb = StringBuilder(message)
            for (token in tokens) {
                if (token !is Token) {
                    continue
                }
                if (token !is SemicolonToken) {
                    sb.append(" ")
                }
                sb.append(token.getDefinition())
            }
            return ParseException(sb.toString())
        }
    }
}
