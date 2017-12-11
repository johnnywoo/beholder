package ru.agalkin.beholder.threads

const val TIMER_FROM_FIELD = "beholder://timer"

object TimerListener {
    val messageRouter by lazy {
        val thread = TimerListenerThread()
        thread.start()
        thread.router
    }
}
