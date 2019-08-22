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
    init {
        // Если в пуле все треды заняты, задачу выполнит тот тред, который пихает её в очередь.
        // По умолчанию в такой ситуации всё падает (RejectedExecutionException),
        // а так мы просто притормозим на пиковой нагрузке.
        pool.rejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy()
    }

    fun execute(block: () -> Unit) {
        pool.execute(block)
    }

    fun destroy() {
        pool.shutdownNow()
    }
}
