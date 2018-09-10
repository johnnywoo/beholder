package ru.agalkin.beholder.stats

import ru.agalkin.beholder.queue.DataBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class StatsHolder {
    private val stats = ConcurrentHashMap(mapOf(
        "fromUdpMessages" to AtomicLong(),
        "fromUdpMaxBytes" to AtomicLong(),
        "fromUdpTotalBytes" to AtomicLong(),

        "fromTcpMessages" to AtomicLong(),
        "fromTcpMaxBytes" to AtomicLong(),
        "fromTcpTotalBytes" to AtomicLong(),

        "fromTcpNewConnections" to AtomicLong(),

        "packCount" to AtomicLong(),
        "packDurationMaxNanos" to AtomicLong(),
        "packDurationTotalNanos" to AtomicLong(),
        "unpackCount" to AtomicLong(),
        "unpackDurationMaxNanos" to AtomicLong(),
        "unpackDurationTotalNanos" to AtomicLong(),
        "compressCount" to AtomicLong(),
        "compressDurationMaxNanos" to AtomicLong(),
        "compressDurationTotalNanos" to AtomicLong(),
        "compressBeforeTotalBytes" to AtomicLong(),
        "compressAfterTotalBytes" to AtomicLong(),
        "decompressCount" to AtomicLong(),
        "decompressDurationMaxNanos" to AtomicLong(),
        "decompressDurationTotalNanos" to AtomicLong(),

        "messagesReceived" to AtomicLong(),
        "queueOverflows" to AtomicLong(),
        "queueMaxSize" to AtomicLong(),
        "queueChunksCreated" to AtomicLong(),
        "defaultBufferMaxBytes" to AtomicLong(),
        "defaultBufferAllocatedBytes" to AtomicLong(),
        "allBuffersMaxBytes" to AtomicLong(),
        "allBuffersAllocatedBytes" to AtomicLong(),
        "configReloads" to AtomicLong(),
        "unparsedDropped" to AtomicLong()
    ))

    fun reportUdpReceived(size: Long) {
        stats["messagesReceived"]?.incrementAndGet()
        stats["fromUdpMessages"]?.incrementAndGet()
        stats["fromUdpMaxBytes"]?.updateAndGet { max(it, size) }
        stats["fromUdpTotalBytes"]?.addAndGet(size)
    }

    fun reportTcpReceived(size: Long) {
        stats["messagesReceived"]?.incrementAndGet()
        stats["fromTcpMessages"]?.incrementAndGet()
        stats["fromTcpMaxBytes"]?.updateAndGet { max(it, size) }
        stats["fromTcpTotalBytes"]?.addAndGet(size)
    }

    fun reportTcpConnected() {
        stats["fromTcpNewConnections"]?.incrementAndGet()
    }

    fun reportQueueOverflow(droppedNumber: Long) {
        stats["queueOverflows"]?.addAndGet(droppedNumber)
    }

    fun reportQueueSize(size: Long) {
        stats["queueMaxSize"]?.updateAndGet { max(it, size) }
    }

    fun reportConfigReload() {
        stats["configReloads"]?.incrementAndGet()
    }

    fun reportUnparsedDropped() {
        stats["unparsedDropped"]?.incrementAndGet()
    }

    fun reportBufferAllocation(buffer: DataBuffer, currentMemoryBytes: Long, allBuffersMemoryBytes: Long, allocatedBytes: Long) {
        stats["allBuffersMaxBytes"]?.updateAndGet { max(it, allBuffersMemoryBytes) }
        stats["allBuffersAllocatedBytes"]?.addAndGet(allocatedBytes)
        if (buffer.id == "") {
            stats["defaultBufferMaxBytes"]?.updateAndGet { max(it, currentMemoryBytes) }
            stats["defaultBufferAllocatedBytes"]?.addAndGet(allocatedBytes)
        }
    }

    fun reportChunkCreated() {
        stats["queueChunksCreated"]?.incrementAndGet()
    }

    fun reportPackTime(spentNanos: Long) {
        stats["packCount"]?.incrementAndGet()
        stats["packDurationMaxNanos"]?.updateAndGet { max(it, spentNanos) }
        stats["packDurationTotalNanos"]?.addAndGet(spentNanos)
    }

    fun reportUnpackTime(spentNanos: Long) {
        stats["unpackCount"]?.incrementAndGet()
        stats["unpackDurationMaxNanos"]?.updateAndGet { max(it, spentNanos) }
        stats["unpackDurationTotalNanos"]?.addAndGet(spentNanos)
    }

    fun reportCompressTime(spentNanos: Long, originalLength: Int, compressedLength: Int) {
        stats["compressCount"]?.incrementAndGet()
        stats["compressDurationMaxNanos"]?.updateAndGet { max(it, spentNanos) }
        stats["compressDurationTotalNanos"]?.addAndGet(spentNanos)
        stats["compressBeforeTotalBytes"]?.addAndGet(originalLength.toLong())
        stats["compressAfterTotalBytes"]?.addAndGet(compressedLength.toLong())
    }

    fun reportDecompressTime(spentNanos: Long) {
        stats["decompressCount"]?.incrementAndGet()
        stats["decompressDurationMaxNanos"]?.updateAndGet { max(it, spentNanos) }
        stats["decompressDurationTotalNanos"]?.addAndGet(spentNanos)
    }

    fun getStatValuesAndReset(): Map<String, Long> {
        val runtime = Runtime.getRuntime()

        val heapSize   = runtime.totalMemory()
        val heapMax    = runtime.maxMemory()
        val heapUnused = runtime.freeMemory()

        val heapUsed = heapSize - heapUnused

        val uptimeSeconds = (System.currentTimeMillis() - uptimeDate.time) / 1000

        val statValues = mutableMapOf(
            "uptimeSeconds" to uptimeSeconds,
            "heapBytes" to heapSize,
            "heapUsedBytes" to heapUsed,
            "heapMaxBytes" to heapMax
        )
        for ((k, v) in stats) {
            statValues[k] = v.getAndSet(0)
        }
        return statValues
    }

    companion object {
        private val uptimeDate = Date()

        fun start() {
            // noop, but it will initialize the uptime date
        }
    }
}
