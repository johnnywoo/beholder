package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.*
import ru.agalkin.beholder.listToString

/**
 * Выражение (команда) из токенов
 *
 * command arg arg;
 * или
 * command arg arg { child; child }
 */
abstract class CommandAbstract(private val arguments: Arguments) {
    abstract protected fun createSubcommand(args: Arguments) : CommandAbstract?

    open fun start() {
        for (command in subcommands) {
            command.start()
        }
    }

    open fun stop() {
        for (command in subcommands) {
            command.stop()
        }
    }

    val receivers = mutableSetOf<(Message) -> Unit>()

    open fun emit(message: Message) {
        for (receiver in receivers) {
            receiver(message)
        }
    }


    protected val subcommands = ArrayList<CommandAbstract>()

    protected fun importSubcommands(tokens: ListIterator<Token>) {
        // Общая логика такая: пытаемся начать команду и присобачить в неё все токены,
        // какие влезают. Сначала она получает аргументы, а потом возможно ещё и детей.
        // Если мы нашли терминатор (; либо закрылся {блок детей}),
        // то поехала рекурсия (ищем следующих детей текущей команды).

        // для начала нам нужен литерал с названием команды
        // сдвигать курсор итератора здесь нельзя, потому что если next не литерал, то мы слопаем чужой токен
        val nameToken = peekNext(tokens)
        if (nameToken !is LiteralToken) {
            return
        }
        // токен нашелся, сдвигаем курсор
        tokens.next()

        val subcommandArgs = ArrayList<ArgumentToken>()
        subcommandArgs.add(nameToken)

        // ок, команду начали, теперь запихиваем в неё все что можно и потом выходим/рекурсия
        while (tokens.hasNext()) {
            val token = tokens.next()

            // аргумент (литерал / кавычки) = просто добавляем в аргументы и едем дальше
            if (token is ArgumentToken) {
                subcommandArgs.add(token)
                continue
            }

            // аргументы кончились = надо зарегистрировать нашу новую команду
            val subcommand: CommandAbstract
            try {
                subcommand = createSubcommand(Arguments(subcommandArgs))
                    ?: throw CommandException("Command `${subcommandArgs[0].getValue()}` is not allowed inside `${arguments.getCommandName()}`")
            } catch (e: CommandException) {
                throw ParseException.fromList(e.message + ":", subcommandArgs)
            }
            subcommands.add(subcommand)

            // нащупали ; = детей нет, команда кончилась (поехали сканировать следующую команду)
            if (token is SemicolonToken) {
                return importSubcommands(tokens)
            }

            // нащупали открывашку = поехал блок и потом выражение кончилось
            if (token is OpenBraceToken) {
                subcommand.importSubcommands(tokens)
                if (!tokens.hasNext() || tokens.next() !is CloseBraceToken) {
                    throw ParseException.fromIterator("Invalid closing brace placement:", tokens, 1)
                }
                return importSubcommands(tokens)
            }

            // нащупали закрывашку = кончились дети
            if (token is CloseBraceToken) {
                // возвращаем позицию обратно, чтобы снаружи в родителе убедиться в необходимости закрывашки
                // (у рута нет ни открывашек, ни закрывашек)
                tokens.previous()
                return
            }

            throw ParseException.fromIterator("Cannot parse token ${token::class.simpleName}:", tokens, 1)
        }
    }

    fun getChildrenDefinition(indent: String = "")
        = listToString(subcommands, { it.getDefinition(indent) + "\n" })

    private fun getDefinition(indent: String = ""): String {
        val sb = StringBuilder()

        // args
        sb.append(indent).append(arguments.toList().joinToString(" ") { it.getDefinition() })

        // child expressions
        if (subcommands.isEmpty()) {
            sb.append(";")
        } else {
            sb.append(" {\n")
            sb.append(getChildrenDefinition(indent + "    "))
            sb.append(indent).append("}")
        }

        return sb.toString()
    }

    private fun <T> peekNext(tokens: ListIterator<T>): T? {
        if (!tokens.hasNext()) {
            return null
        }
        val next = tokens.next()
        tokens.previous()
        return next
    }
}
