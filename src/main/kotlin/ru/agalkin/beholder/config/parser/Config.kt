package ru.agalkin.beholder.config.parser

class Config(configText: String) {
    init {
        println("=== Config text ===")
        println(configText)
        println("=== End config text ===")

        // читаем символы из строки и формируем токены
        val tokens = Token.getTokens(configText)

        // println("=== Tokens ===")
        // for (token in tokens) {
        //     println("type: ${token.getTypeName()} def: ${token.getDefinition()}")
        // }
        // println("=== End tokens ===")

        // окей, токены получились, теперь надо сделать из них выражения
        // выражение = набор токенов до терминатора (потенциально с детишками)
        // вариант 1: command arg arg;
        // вариант 2: command arg { block }
        val root = Expression.fromTokens(tokens)

        println("=== Parsed config ===")
        println(root.getChildrenDefinition())
        println("=== End parsed config ===")
    }
}

