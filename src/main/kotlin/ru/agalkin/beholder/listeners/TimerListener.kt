package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.getIsoDateFormatter
import java.util.*

class TimerListener {
    private val messageParts = arrayOf(
        listOf("A tiny", "A small", "A little", "An average", "A big", "A large", "A huge", "An enormous"),
        listOf("mouse", "cat", "dog", "elephant", "wolf", "hamster", "chicken", "hedgehog"),
        listOf("entered", "left", "walked by", "is buried under", "is looking to buy", "jumped over", "got fed up with"),
        listOf("the building", "the car", "the Moon", "a cookie jar", "the zoo", "the shark", "the Beatles")
    )

    private val listenerThread = object : Thread("timer-listener") {
        override fun run() {
            InternalLog.info("Thread $name got started")

            var millis = Date().time

            while (true) {
                val message = Message()

                message["receivedDate"]  = curDateIso()
                message["syslogProgram"] = "beholder"
                message["from"]          = FROM_FIELD_VALUE

                val sb = StringBuilder()
                for (set in messageParts) {
                    if(!sb.isEmpty()) {
                        sb.append(' ')
                    }
                    sb.append(set.shuffled()[0])
                }
                message["payload"] = sb.toString()

                for (receiver in receivers) {
                    receiver(message)
                }

                // выдаём сообщения точно раз в секунду, без накопления ошибки
                millis += 1000
                sleep(millis - Date().time)
            }
        }

        private val formatter = getIsoDateFormatter()

        private fun curDateIso(): String
            = formatter.format(Date())
    }

    init {
        listenerThread.start()
    }

    val receivers = mutableSetOf<(Message) -> Unit>()

    companion object {
        const val FROM_FIELD_VALUE = "beholder://timer"

        val instance: TimerListener by lazy { TimerListener() }
    }
}
