package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Message
import java.text.SimpleDateFormat
import java.util.*

class TimerListener {
    private val listenerThread = object : Thread("timer-listener") {
        override fun run() {
            println("Thread $name got started")

            while (true) {
                for (receiver in receivers) {
                    val message = Message()

                    message["receivedDate"] = curDateIso()
                    message["from"]         = "timer://timer"

                    receiver(message)
                }

                sleep(1000)
            }
        }

        // 2017-11-26T16:16:01+03:00
        // 2017-11-26T16:16:01Z if UTC
        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

        private fun curDateIso(): String
            = formatter.format(Date())
    }

    init {
        listenerThread.start()
    }

    val receivers = mutableSetOf<(Message) -> Unit>()

    companion object {
        val timer = TimerListener()
    }
}
