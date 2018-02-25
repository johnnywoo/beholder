package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import java.util.concurrent.atomic.AtomicBoolean

class QueueEmitterThread(
    private val shouldStop: AtomicBoolean,
    private val router: MessageRouter,
    private val queue: MessageQueue,
    threadName: String
) : Thread(threadName) {

    private val isPaused = AtomicBoolean(false)

    private val reloadListener = object : Beholder.ReloadListener {
        override fun before(app: Beholder) {
            // перед тем, как заменять конфиг приложения,
            // мы хотим поставить приём сообщений на паузу
            isPaused.set(true)
        }

        override fun after(app: Beholder) {
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

            val message = queue.shift(100) // blocking for 100 millis
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

        queue.destroy()

        Beholder.reloadListeners.remove(reloadListener)

        InternalLog.info("Thread $name got deleted")
    }
}
