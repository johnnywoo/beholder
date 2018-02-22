package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class QueueEmitterThread(
    private val shouldStop: AtomicBoolean,
    private val router: MessageRouter,
    threadName: String
) : Thread(threadName) {
    val queue = LinkedBlockingQueue<Message>()

    private val isPaused = AtomicBoolean(false)

    private val reloadListener = object : Beholder.ReloadListener {
        override fun before() {
            // перед тем, как заменять конфиг приложения,
            // мы хотим поставить приём сообщений на паузу
            isPaused.set(true)
        }

        override fun after() {
            isPaused.set(false)
        }
    }

    init {
        Beholder.reloadListeners.add(reloadListener)
    }

    override fun run() {
        InternalLog.info("Thread $name got started")

        // эмиттер останавливается, когда подписчики кончились
        while (!shouldStop.get()) {
            if (isPaused.get()) {
                Thread.sleep(50)
                continue
            }

            val message = queue.poll(100, TimeUnit.MILLISECONDS) // blocking for 100 millis
            if (message == null) {
                // за 100 мс ничего не нашли
                // проверим все условия и поедем ждать заново
                continue
            }

            // выхватили сообщение, а эмиттер уже на паузе — надо обождать
            while (isPaused.get()) {
                Thread.sleep(50)
            }

            router.sendMessageToSubscribers(message)
        }

        // на всякий случай, если мы будем перезапускать лисенер, надо тут всё зачистить
        queue.clear()

        Beholder.reloadListeners.remove(reloadListener)

        InternalLog.info("Thread $name got deleted")
    }
}
