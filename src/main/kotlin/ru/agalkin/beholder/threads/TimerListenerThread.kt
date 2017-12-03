package ru.agalkin.beholder.threads

import ru.agalkin.beholder.*
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

class TimerListenerThread : Thread("timer-listener") {
    val receivers = CopyOnWriteArraySet<(Message) -> Unit>()

    override fun run() {
        InternalLog.info("Thread $name got started")

        var millis = Date().time

        while (true) {
            val message = Message()

            message["receivedDate"]  = curDateIso()
            message["syslogProgram"] = BEHOLDER_SYSLOG_PROGRAM
            message["from"]          = TIMER_FROM_FIELD

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
            val afterLoopTime = Date().time
            while (millis - afterLoopTime <= 0) {
                millis += 1000
            }
            sleep(millis - afterLoopTime)
        }
    }

    private val formatter = getIsoDateFormatter()

    private fun curDateIso(): String
        = formatter.format(Date())

    companion object {
        private val messageParts = arrayOf(
            listOf("A tiny", "A small", "A little", "An average", "A big", "A large", "A huge", "An enormous"),
            listOf("mouse", "cat", "dog", "elephant", "wolf", "hamster", "chicken", "hedgehog"),
            listOf("entered", "left", "walked by", "is buried under", "is looking to buy", "jumped over", "got fed up with"),
            listOf("the building", "the car", "the Moon", "a cookie jar", "the zoo", "the shark", "the Beatles")
        )
    }
}
