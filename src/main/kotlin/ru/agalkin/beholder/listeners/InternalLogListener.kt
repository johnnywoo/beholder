package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class InternalLogListener {
    private val queue = LinkedBlockingQueue<Message>()

    private var isEmitterPaused by ThreadSafeFlag(false)

    private val emitterThread = object : Thread("internal-log-emitter") {
        override fun run() {
            InternalLog.info("Thread $name got started")

            // эмиттер не умирает (только ставится иногда на паузу), потому что незачем
            while (true) {
                if (isEmitterPaused) {
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
                while (isEmitterPaused) {
                    Thread.sleep(50)
                    continue
                }

                for (receiver in receivers) {
                    receiver(message)
                }
            }
        }
    }

    init {
        Beholder.reloadListeners.add(object : Beholder.ReloadListener {
            override fun before() {
                // перед тем, как заменять конфиг приложения,
                // мы хотим поставить приём сообщений на паузу
                isEmitterPaused = true
            }

            override fun after() {
                isEmitterPaused = false
            }
        })

        emitterThread.start()
    }

    val receivers = mutableSetOf<(Message) -> Unit>()

    fun add(message: Message)
        = queue.offer(message)

    companion object {
        var isInitialized = false

        val instance: InternalLogListener by lazy {
            if (!isInitialized) {
                isInitialized = true
            }
            InternalLogListener()
        }
    }

    private class ThreadSafeFlag(initialValue: Boolean) : ReadWriteProperty<Any, Boolean> {
        private val ab = AtomicBoolean(initialValue)

        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean
            = ab.get()

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean)
            = ab.set(value)
    }
}
