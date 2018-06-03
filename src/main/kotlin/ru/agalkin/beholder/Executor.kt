package ru.agalkin.beholder

import java.util.concurrent.Executors

object Executor {
    private val pool = Executors.newCachedThreadPool()

    fun execute(block: () -> Unit) {
        pool.execute(block)
    }
}
