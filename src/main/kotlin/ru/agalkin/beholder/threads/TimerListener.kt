package ru.agalkin.beholder.threads

const val TIMER_FROM_FIELD = "beholder://timer"

object TimerListener {
    val subscribers by lazy {
        val thread = TimerListenerThread()
        thread.start()
        thread.router.subscribers
    }
}
