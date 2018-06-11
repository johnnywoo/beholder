package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.*
import ru.agalkin.beholder.queue.BeholderQueue
import java.util.concurrent.atomic.AtomicBoolean

class QueueEmitterThread(
    private val app: Beholder,
    private val shouldStop: AtomicBoolean,
    private val router: MessageRouter,
    private val queue: BeholderQueue<Message>,
    threadName: String
) : Thread(threadName) {

    private val isPaused = AtomicBoolean(false)

    // перед тем, как заменять конфиг приложения,
    // мы хотим поставить приём сообщений на паузу
    private val beforeReload = { isPaused.set(true) }
    private val afterReload  = { isPaused.set(false) }

    init {
        app.beforeReloadCallbacks.add(beforeReload)
        app.afterReloadCallbacks.add(afterReload)
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

        app.beforeReloadCallbacks.remove(beforeReload)
        app.afterReloadCallbacks.remove(afterReload)

        InternalLog.info("Thread $name got deleted")
    }
}
