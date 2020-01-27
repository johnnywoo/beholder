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

        "toUdpMessages" to AtomicLong(),
        "toUdpMaxBytes" to AtomicLong(),
        "toUdpTotalBytes" to AtomicLong(),

        "toTcpMessages" to AtomicLong(),
        "toTcpMaxBytes" to AtomicLong(),
        "toTcpTotalBytes" to AtomicLong(),

        "toFileMessages" to AtomicLong(),
        "toFileMaxBytes" to AtomicLong(),
        "toFileTotalBytes" to AtomicLong(),

        "toShellMessages" to AtomicLong(),
        "toShellMaxBytes" to AtomicLong(),
        "toShellTotalBytes" to AtomicLong(),

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
        "messagesSent" to AtomicLong(),
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

    fun reportUdpSent(size: Long) {
        stats["messagesSent"]?.incrementAndGet()
        stats["toUdpMessages"]?.incrementAndGet()
        stats["toUdpMaxBytes"]?.updateAndGet { max(it, size) }
        stats["toUdpTotalBytes"]?.addAndGet(size)
    }

    fun reportTcpSent(size: Long) {
        stats["messagesSent"]?.incrementAndGet()
        stats["toTcpMessages"]?.incrementAndGet()
        stats["toTcpMaxBytes"]?.updateAndGet { max(it, size) }
        stats["toTcpTotalBytes"]?.addAndGet(size)
    }

    fun reportFileSent(size: Long) {
        stats["messagesSent"]?.incrementAndGet()
        stats["toFileMessages"]?.incrementAndGet()
        stats["toFileMaxBytes"]?.updateAndGet { max(it, size) }
        stats["toFileTotalBytes"]?.addAndGet(size)
    }

    fun reportShellSent(size: Long) {
        stats["messagesSent"]?.incrementAndGet()
        stats["toShellMessages"]?.incrementAndGet()
        stats["toShellMaxBytes"]?.updateAndGet { max(it, size) }
        stats["toShellTotalBytes"]?.addAndGet(size)
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

    fun reportBufferSizeChange(buffer: DataBuffer, currentMemoryBytes: Long, allBuffersMemoryBytes: Long, allocatedBytes: Long) {
        stats["allBuffersMaxBytes"]?.updateAndGet { max(it, allBuffersMemoryBytes) }
        if (allocatedBytes > 0) {
            stats["allBuffersAllocatedBytes"]?.addAndGet(allocatedBytes)
        }
        if (buffer.id == "") {
            stats["defaultBufferMaxBytes"]?.updateAndGet { max(it, currentMemoryBytes) }
            if (allocatedBytes > 0) {
                stats["defaultBufferAllocatedBytes"]?.addAndGet(allocatedBytes)
            }
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

    /**
     * Names of stats that are counting something (and not reporting gauge-like values)
     */
    fun getCountingStatNames(): List<String> {
        return stats.keys().toList() + "uptimeSeconds"
    }

    fun getDescriptions(): Map<String, String> {
        return mapOf(
            "allBuffersAllocatedBytes" to "Total amount of bytes added to all buffers (does not decrease when memory is released)",
            "allBuffersMaxBytes" to "Maximum buffer size, total for all buffers",
            "compressAfterTotalBytes" to "Total size of all data fed into compressors",
            "compressBeforeTotalBytes" to "Total size of data produced by compressors",
            "compressCount" to "Number of compress operations (chunk moves from queue to buffer)",
            "compressDurationMaxNanos" to "Max duration of a compress",
            "compressDurationTotalNanos" to "Total duration of all compress operations",
            "configReloads" to "Number of successful config reloads",
            "decompressCount" to "Number of decompress operations (chunk moves from buffer to queue)",
            "decompressDurationMaxNanos" to "Max duration of a decompress",
            "decompressDurationTotalNanos" to "Total duration of all decompress operations",
            "defaultBufferAllocatedBytes" to "Total amount of bytes added to the default buffer (does not decrease when memory is released)",
            "defaultBufferMaxBytes" to "Maximum size of the default buffer",
            "fromTcpMaxBytes" to "Maximum length of a message received over TCP",
            "fromTcpMessages" to "Number of messages received over TCP",
            "fromTcpNewConnections" to "Number of accepted TCP connections",
            "fromTcpTotalBytes" to "Total number of bytes received over TCP",
            "fromUdpMaxBytes" to "Maximum length of a packet received over UDP",
            "fromUdpMessages" to "Number of messages received over UDP",
            "fromUdpTotalBytes" to "Summed length of all packets received over UDP",
            "toTcpMaxBytes" to "Maximum length of a message sent over TCP",
            "toTcpMessages" to "Number of messages sent over TCP",
            "toTcpTotalBytes" to "Total number of bytes sent over TCP",
            "toUdpMaxBytes" to "Maximum length of a packet sent over UDP",
            "toUdpMessages" to "Number of messages sent over UDP",
            "toUdpTotalBytes" to "Summed length of all packets sent over UDP",
            "toFileMaxBytes" to "Maximum length that was written into a file",
            "toFileMessages" to "Number of messages written into files",
            "toFileTotalBytes" to "Summed length of all messages written into files",
            "toShellMaxBytes" to "Maximum length that was written into a shell command",
            "toShellMessages" to "Number of messages written into shell commands",
            "toShellTotalBytes" to "Summed length of all messages written into shell commands",
            "heapBytes" to "Current heap size in bytes (memory usage)",
            "heapMaxBytes" to "Maximal heap size",
            "heapUsedBytes" to "Used memory in the heap",
            "messagesReceived" to "Count of received messages",
            "messagesSent" to "Count of sent messages",
            "packCount" to "Number of pack operations (chunk moves from queue to buffer)",
            "packDurationMaxNanos" to "Max duration of a pack",
            "packDurationTotalNanos" to "Total duration of all pack operations",
            "queueChunksCreated" to "Number of queue chunks created",
            "queueMaxSize" to "Maximum size of a queue",
            "queueOverflows" to "Number of messages dropped due to a queue overflow",
            "unpackCount" to "Number of unpacks (chunk moves from buffer to queue)",
            "unpackDurationMaxNanos" to "Max duration of an unpack",
            "unpackDurationTotalNanos" to "Total duration of all unpacks",
            "unparsedDropped" to "Number of messages dropped due to parse errors",
            "uptimeSeconds" to "Uptime in seconds"
        )
    }

    private fun getNonResettingStatValues(): MutableMap<String, Long> {
        val runtime = Runtime.getRuntime()

        val heapSize   = runtime.totalMemory()
        val heapMax    = runtime.maxMemory()
        val heapUnused = runtime.freeMemory()

        val heapUsed = heapSize - heapUnused

        val uptimeSeconds = (System.currentTimeMillis() - uptimeDate.time) / 1000

        return mutableMapOf(
            "uptimeSeconds" to uptimeSeconds,
            "heapBytes" to heapSize,
            "heapUsedBytes" to heapUsed,
            "heapMaxBytes" to heapMax
        )
    }

    fun getStatValues(): Map<String, Long> {
        val statValues = getNonResettingStatValues()
        for ((k, v) in stats) {
            statValues[k] = v.get()
        }
        return statValues
    }

    fun getStatValuesAndReset(): Map<String, Long> {
        val statValues = getNonResettingStatValues()
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
