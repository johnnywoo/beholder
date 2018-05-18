package ru.agalkin.beholder

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DataQueue(capacityOption: ConfigOption) {
    private val queue = LinkedBlockingQueue<FieldValue>()

    private val maxMessages = AtomicInteger(1000)

    private val reloadListener = object : Beholder.ReloadListener {
        override fun before(app: Beholder) {}

        override fun after(app: Beholder) {
            maxMessages.set(app.config.getIntOption(capacityOption))
        }
    }
    init {
        Beholder.reloadListeners.add(reloadListener)
    }

    fun add(chunk: FieldValue) {
        while (queue.size >= maxMessages.get()) {
            queue.take()
        }
        queue.offer(chunk)
    }

    fun shift(timeoutMillis: Long): FieldValue? {
        return queue.poll(timeoutMillis, TimeUnit.MILLISECONDS)
    }
}