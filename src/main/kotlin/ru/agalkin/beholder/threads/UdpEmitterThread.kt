package ru.agalkin.beholder.threads

import ru.agalkin.beholder.InternalLog
import java.util.concurrent.TimeUnit

class UdpEmitterThread(private val udpListener: UdpListener) : Thread("from-udp-${udpListener.address}-emitter") {
    override fun run() {
        InternalLog.info("Thread $name got started")

        // эмиттер останавливается, когда подписчики кончились
        while (!udpListener.isListenerDeleted.get()) {
            if (udpListener.isEmitterPaused.get()) {
                Thread.sleep(50)
                continue
            }

            val message = udpListener.queue.poll(100, TimeUnit.MILLISECONDS) // blocking for 100 millis
            if (message == null) {
                // за 100 мс ничего не нашли
                // проверим все условия и поедем ждать заново
                continue
            }

            // выхватили сообщение, а эмиттер уже на паузе — надо обождать
            while (udpListener.isEmitterPaused.get()) {
                Thread.sleep(50)
            }

            for (receiver in udpListener.receivers) {
                receiver(message)
            }
        }

        // на всякий случай, если мы будем перезапускать лисенер, надо тут всё зачистить
        udpListener.queue.clear()

        InternalLog.info("Thread $name got deleted")
    }
}
