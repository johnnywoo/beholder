package ru.agalkin.beholder.listeners

const val TIMER_FROM_FIELD = "beholder://timer"

object TimerListener {
    val messageRouter by lazy {
        val thread = TimerListenerThread()
        thread.start()
        thread.router
    }
}
