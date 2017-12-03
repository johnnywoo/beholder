package ru.agalkin.beholder.threads

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.MessageRouter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class InternalLogEmitterThread : Thread("internal-log-emitter") {
    val isEmitterPaused = AtomicBoolean(false)

    val queue = LinkedBlockingQueue<Message>()

    companion object {
        val router = MessageRouter()
    }

    override fun run() {
        InternalLog.info("Thread $name got started")

        // эмиттер не умирает (только ставится иногда на паузу), потому что незачем
        while (true) {
            if (isEmitterPaused.get()) {
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
            while (isEmitterPaused.get()) {
                Thread.sleep(50)
            }

            router.sendMessageToSubscribers(message)
        }
    }
}
