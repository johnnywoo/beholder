package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder

const val TIMER_FROM_FIELD = "beholder://timer"

class TimerListener(private val app: Beholder) {
    val messageRouter by lazy {
        val thread = TimerListenerThread(app)
        thread.start()
        thread.router
    }
}
