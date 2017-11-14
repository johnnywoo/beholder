package ru.agalkin.beholder.config.parser

class ParseException(message: String, tokens: Iterator<Token>) : RuntimeException(composeMessage(message, tokens)) {
    companion object {
        private fun composeMessage(message: String, tokens: Iterator<Token>): String {
            val sb = StringBuilder(message)
            while (tokens.hasNext()) {
                val token = tokens.next()
                if (token !is SemicolonToken) {
                    sb.append(" ")
                }
                sb.append(token.getDefinition())
            }
            return sb.toString()
        }
    }
}
