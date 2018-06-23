package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class DataBuffer(app: Beholder) {
    private val maxTotalSize = AtomicInteger(app.getIntOption(ConfigOption.BUFFER_MEMORY_BYTES))
    init {
        app.afterReloadCallbacks.add {
            maxTotalSize.set(app.getIntOption(ConfigOption.BUFFER_MEMORY_BYTES))
        }
    }

    val currentSizeInMemory = AtomicInteger(0)

    private val byteArrays: Deque<ByteArray> = LinkedList<ByteArray>()

    fun allocate(size: Int): WeakReference<ByteArray> {
        synchronized(this) {
            while (currentSizeInMemory.get() + size > maxTotalSize.get()) {
                val removed = byteArrays.pollFirst()
                currentSizeInMemory.addAndGet(-removed.size)
            }

            val bytes = ByteArray(size)
            byteArrays.addLast(bytes)
            currentSizeInMemory.addAndGet(size)

            return WeakReference(bytes)
        }
    }

    fun release(ref: WeakReference<ByteArray>)
        = release(ref.get())

    fun release(byteArray: ByteArray?) {
        if (byteArray == null) {
            return
        }
        synchronized(this) {
            val isRemoved = byteArrays.remove(byteArray)
            if (isRemoved) {
                currentSizeInMemory.addAndGet(-byteArray.size)
            }
        }
    }
}
