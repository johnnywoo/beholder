package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.lang.ref.WeakReference
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue
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

    private fun createChunk(capacity: Int? = null): Queue<T> {
        return ArrayBlockingQueue(capacity ?: app.getIntOption(ConfigOption.QUEUE_CHUNK_MESSAGES))
    }

    @Volatile private var inboundChunk = createChunk()
    @Volatile private var outboundChunk = inboundChunk
    init {
        Stats.reportChunkCreated()
    }

    private val bufferedChunks: Queue<WeakReference<DataBuffer.Buffered>> = ArrayDeque()

    fun add(message: T) {
        if (!inboundChunk.offer(message)) {
            // Не удалось засунуть элемент в кусок. Значит он наполнился.
            // Надо сделать новый.
            synchronized(this) {
                // Возможно, другой тред уже создал новый кусок тут.
                if (inboundChunk.offer(message)) {
                    return@synchronized
                }

                // Текущий кусок всё ещё полный, создаём новый кусок и пихаем сообщение в него.

                if (inboundChunk !== outboundChunk) {
                    // Мы пытаемся создать третий (или больше) кусок очереди.
                    // Все куски кроме первого и последнего мы пихаем в буфер.
                    // 1. Буфер может выкинуть наш кусок, если там не хватает выделенной под это памяти
                    // 2. В буфере данные можно держать сжатыми и экономить эту самую память
                    val list = inboundChunk.toList()
                    val messageCount = list.count()

                    val bytes = Stats.timePackProcess { pack(list) }
                    val originalSize = bytes.size

                    val compressor = buffer.compressor
                    val compressedBytes = Stats.timeCompressProcess(originalSize) { compressor.compress(bytes) }

                    bufferedChunks.offer(buffer.allocate(
                        compressedBytes,
                        originalSize,
                        compressor,
                        {
                            // Эта лямбда вызовется, если данные умерли в буфере. Корректируем метрики.
                            val unusedItemsNumber = messageCount.toLong()
                            Stats.reportQueueOverflow(unusedItemsNumber)
                            val size = totalMessagesCount.addAndGet(-unusedItemsNumber)
                            Stats.reportQueueSize(size)
                        }
                    ))
                }

                // Поскольку две параллельные записи пришли из разных тредов,
                // мы всё равно не знаем, какая вставка должна была быть раньше,
                // так что делаем вывод, что между ними порядок вставки можно не соблюдать.
                inboundChunk = createChunk()
                Stats.reportChunkCreated()
                if (!inboundChunk.offer(message)) {
                    throw BeholderException("Fresh queue chunk is immediately full")
                }
            }
        }

        val size = totalMessagesCount.incrementAndGet()
        Stats.reportQueueSize(size)

        startPolling()
    }

    private fun switchOutboundChunk(): Boolean {
        synchronized(this) {
            if (inboundChunk === outboundChunk) {
                return false
            }

            // Пытаемся восстановить кусок из буфера.
            while (true) {
                val bufferedRef = bufferedChunks.poll()
                if (bufferedRef == null) {
                    // В буфере пусто.
                    break
                }

                val buffered = bufferedRef.get()
                if (buffered == null) {
                    // В буфере что-то было, но не выжило.
                    continue
                }

                // Достали байты из буфера. Делаем из них кусок очереди.
                val compressedBytes = buffered.release()
                val bytes = Stats.timeDecompressProcess { buffered.compressor.decompress(compressedBytes, buffered.originalSize) }
                val list = Stats.timeUnpackProcess { unpack(bytes) }
                outboundChunk = createChunk(list.count())
                if (!outboundChunk.addAll(list)) {
                    throw BeholderException("Could not fit buffered data into queue chunk")
                }
                return true
            }

            // Буфер пустой, значит просто пихаем входную очередь на выход.
            outboundChunk = inboundChunk
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
                    val message = outboundChunk.poll()
                    if (message == null) {
                        // Кусок пуст. Посмотрим, есть ли у нас другой кусок.
                        if (switchOutboundChunk()) {
                            // Удалось подключить новый кусок, можно разгребать сообщения дальше.
                            continue
                        }

                        // Нового куска не нашлось, значит сообщения кончились совсем.
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
