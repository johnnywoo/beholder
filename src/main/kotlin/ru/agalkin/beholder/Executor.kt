package ru.agalkin.beholder

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Executor {
    private val pool = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        2000,
        60, TimeUnit.SECONDS,
        SynchronousQueue()
    )

    fun execute(block: () -> Unit) {
        pool.execute(block)
    }

    fun destroy() {
        pool.shutdownNow()
    }
}
