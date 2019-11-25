package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.compressors.Compressor
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.lang.ref.WeakReference
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

abstract class BeholderQueueAbstract<T>(
    protected val app: Beholder,
    private val receive: (T) -> Received
) {
    private val buffer by lazy { app.defaultBuffer }

    private val totalMessagesCount = AtomicLong()

    // перед тем, как заменять конфиг приложения,
    // мы хотим поставить приём сообщений на паузу
    private val isPaused = AtomicBoolean(false)

    fun getIsPausedOnlyForTests(): AtomicBoolean {
        return isPaused
    }

    init {
        app.beforeReloadCallbacks.add {
            isPaused.set(true)
        }
        app.afterReloadCallbacks.add {
            isPaused.set(false)
            startPolling()
        }
    }

    abstract fun pack(list: List<T>): ByteArray
    abstract fun unpack(bytes: ByteArray): List<T>

    private fun createChunkQueue(capacity: Int? = null): ArrayBlockingQueue<T> {
        return ArrayBlockingQueue(capacity ?: app.getIntOption(ConfigOption.QUEUE_CHUNK_MESSAGES))
    }

    @Volatile private var inboundQueue = createChunkQueue()
    @Volatile private var outboundQueue = inboundQueue
    init {
        Stats.reportChunkCreated()
    }

    private val bufferedQueue = ConcurrentLinkedQueue<Buffered>()

    private class Buffered(
        val allocated: WeakReference<ByteArray>,
        val originalLength: Int,
        val compressor: Compressor,
        val messageCount: Int
    )

    fun add(message: T) {
        val queue = inboundQueue
        if (!queue.offer(message)) {
            // Не удалось засунуть элемент в очередь. Значит она наполнилась.
            // Надо сделать новую.
            synchronized(this) {
                // Возможно, другой тред уже создал новую очередь тут.
                if (inboundQueue.offer(message)) {
                    return@synchronized
                }

                // Очередь всё ещё пуста, создаём новый кусок и пихаем сообщение в него.

                if (inboundQueue !== outboundQueue) {
                    // Мы пытаемся создать третий кусок очереди.
                    // Тут надо поработать с буфером.
                    val list = inboundQueue.toList()
                    val bytes = Stats.timePackProcess { pack(list) }
                    val compressor = buffer.compressor
                    val compressedBytes = Stats.timeCompressProcess(bytes.size) { compressor.compress(bytes) }
                    val allocated = buffer.allocate(compressedBytes)
                    bufferedQueue.offer(Buffered(
                        allocated,
                        bytes.size,
                        compressor,
                        list.count()
                    ))
                }

                // Поскольку две параллельные записи пришли из разных тредов,
                // мы всё равно не знаем, какая вставка должна была быть раньше,
                // так что делаем вывод, что между ними порядок вставки можно не соблюдать.
                inboundQueue = createChunkQueue()
                if (!inboundQueue.offer(message)) {
                    throw BeholderException("Fresh queue chunk is immediately full")
                }
                Stats.reportChunkCreated()
            }
        }

        val size = totalMessagesCount.incrementAndGet()
        Stats.reportQueueSize(size)

        startPolling()
    }

    private fun switchOutboundQueue(): Boolean {
        synchronized(this) {
            if (inboundQueue === outboundQueue) {
                return false
            }

            // Пытаемся восстановить кусок из буфера.
            while (true) {
                val buffered = bufferedQueue.poll()
                if (buffered == null) {
                    // В буфере пусто.
                    break
                }

                val compressedBytes = buffered.allocated.get()
                if (compressedBytes == null) {
                    // В буфере что-то было, но не выжило.
                    // Корректируем метрики и пробуем достать следующий буферизованный кусок.
                    val unusedItemsNumber = buffered.messageCount.toLong()
                    val size = totalMessagesCount.addAndGet(-unusedItemsNumber)
                    Stats.reportQueueSize(size)
                    Stats.reportQueueOverflow(unusedItemsNumber)

                    continue
                }

                buffer.release(buffered.allocated)

                // Достали байты из буфера. Делаем из них кусок очереди.
                val bytes = Stats.timeDecompressProcess { buffered.compressor.decompress(compressedBytes, buffered.originalLength) }
                val list = Stats.timeUnpackProcess { unpack(bytes) }
                outboundQueue = createChunkQueue(list.count())
                if (!outboundQueue.addAll(list)) {
                    throw BeholderException("Could not fit buffered data into queue chunk")
                }
                return true
            }

            // Буфер пустой, значит просто пихаем входную очередь на выход.
            outboundQueue = inboundQueue
            return true
        }
    }

    private val isPolling = AtomicBoolean(false)

    private fun startPolling() {
        // Пауза = ничего не делаем (снятие с паузы вызовет startPolling() заново)
        if (isPaused.get()) {
            return
        }

        // Если уже есть запущенный цикл обработки сообщений, ничего не делаем
        if (isPolling.compareAndExchange(false, true)) {
            return
        }

        // Мы включили isPolling и теперь запускаем цикл разбора очереди.
        // Будем разбирать до пустой очереди либо до постановки разбора на паузу.
        app.executor.execute {
            try {
                while (!isPaused.get()) {
                    val message = outboundQueue.poll()
                    if (message == null) {
                        // Очередь пуста. Посмотрим, есть ли у нас другая очередь.
                        if (switchOutboundQueue()) {
                            // Удалось подключить новую очередь, можно разгребать сообщения дальше.
                            continue
                        }

                        // Новой очереди не нашлось, значит сообщения кончились совсем.
                        // Выключаем isPolling. Добавление нового сообщения включит его обратно.
                        isPolling.set(false)
                        return@execute
                    }

                    while (true) {
                        val result = receive(message)
                        if (result == Received.OK) {
                            break
                        }
                        // Received.RETRY
                    }

                    // Сообщение успешно уехало из очереди = обновляем метрики.
                    val size = totalMessagesCount.decrementAndGet()
                    Stats.reportQueueSize(size)

                    // Теперь можно брать новое сообщение из очереди и работать с ним.
                }
            } finally {
                isPolling.set(false)
            }
        }
    }
}
