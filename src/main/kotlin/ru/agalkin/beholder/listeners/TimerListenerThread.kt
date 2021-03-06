package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import java.util.*

class TimerListenerThread(private val app: Beholder) : Thread("timer-listener") {
    val router = MessageRouter()

    override fun run() {
        InternalLog.info("Thread $name was started")

        var millis = Date().time

        while (true) {
            router.sendUniqueMessagesToSubscribers {
                val message = Message()

                message["date"]    = app.curDateIso()
                message["program"] = BEHOLDER_SYSLOG_PROGRAM
                message["from"]    = TIMER_FROM_FIELD

                val sb = StringBuilder()
                for (set in messageParts) {
                    if (!sb.isEmpty()) {
                        sb.append(' ')
                    }
                    sb.append(set.shuffled()[0])
                }
                message["payload"] = sb.toString()

                message
            }

            // Выдаём сообщения точно раз в секунду, без накопления ошибки
            val afterLoopTime = Date().time
            while (millis - afterLoopTime <= 0) {
                millis += 1000
            }
            sleep(millis - afterLoopTime)
        }
    }

    companion object {
        private val messageParts = arrayOf(
            listOf("A tiny", "A small", "A little", "An average", "A big", "A large", "A huge", "An enormous"),
            listOf("mouse", "cat", "dog", "elephant", "wolf", "hamster", "chicken", "hedgehog"),
            listOf("entered", "left", "walked by", "is buried under", "is looking to buy", "jumped over", "got fed up with", "got paid by"),
            listOf("the building", "the car", "the Moon", "a cookie jar", "the zoo", "the shark", "the Beatles")
        )
    }
}
