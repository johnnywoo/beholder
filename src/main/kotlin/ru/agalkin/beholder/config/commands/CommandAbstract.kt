package ru.agalkin.beholder.config.commands

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.parser.*

/**
 * Выражение (команда) из токенов
 *
 * command arg arg;
 * или
 * command arg arg { child; child }
 */
abstract class CommandAbstract(protected val arguments: List<ArgumentToken>) {
    abstract protected fun createSubcommand(args: List<ArgumentToken>) : CommandAbstract?

    open fun start() {
        for (command in subcommands) {
            command.start()
        }
    }

    private val receivers = mutableSetOf<(Message) -> Unit>()

    open fun emit(message: Message) {
        for (receiver in receivers) {
            receiver(message)
        }
    }

    fun addReceiver(receiver: (Message) -> Unit)
        = receivers.add(receiver)

    fun removeReceiver(receiver: (Message) -> Unit)
        = receivers.remove(receiver)


    protected fun requireArg(index: Int, errorMessage: String): String {
        if (arguments.size < index) {
            throw CommandException(errorMessage)
        }
        return arguments[index].getValue()
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
            val subcommand = createSubcommand(subcommandArgs)
            if (subcommand == null) {
                rewindIterator(tokens, subcommandArgs.size)
                throw ParseException("Command is not allowed here:", tokens)
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
                    tokens.previous()
                    throw ParseException("Invalid closing brace placement:", tokens)
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

            tokens.previous()
            throw ParseException("Cannot parse token ${token::class.simpleName}:", tokens)
        }
    }

    fun getChildrenDefinition(indent: String = "")
        = listToString(subcommands, { it.getDefinition(indent) + "\n" })

    fun getDefinition(indent: String = ""): String {
        val sb = StringBuilder()

        // args
        sb.append(indent).append(arguments.joinToString(" ") { it.getDefinition() })

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

    private tailrec fun <T> rewindIterator(iterator: ListIterator<T>, offset: Int) {
        if (offset > 0) {
            rewindIterator(iterator, offset - 1)
        }
    }
}
