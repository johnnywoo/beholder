package ru.agalkin.beholder.stats

import ru.agalkin.beholder.queue.DataBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class StatsHolder {
    private val stats = mapOf(
        "fromUdpMessages" to AtomicLong(),
        "fromUdpMaxBytes" to AtomicLong(),
        "fromUdpTotalBytes" to AtomicLong(),

        "fromTcpMessages" to AtomicLong(),
        "fromTcpMaxBytes" to AtomicLong(),
        "fromTcpTotalBytes" to AtomicLong(),

        "fromTcpNewConnections" to AtomicLong(),

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
    )

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

    private fun getStatValuesAndReset(): Map<String, Long> {
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

    fun getFieldsAndReset(): Map<String, String> {
        val statValues = getStatValuesAndReset()
        val fields = mutableMapOf<String, String>()
        val sb = StringBuilder()
        for ((statFieldName, value) in statValues) {
            fields[statFieldName] = value.toString()

            if (!sb.isEmpty()) {
                sb.append(' ')
            }
            sb.append(
                statFieldName
                    .replace("(Bytes|Seconds)$".toRegex(), "")
                    .replace("[A-Z]".toRegex()) { "-" + it.groups[0]?.value?.toLowerCase() }
            )
            sb.append(' ')
            sb.append(when {
                statFieldName.endsWith("Bytes") -> getBytesString(value)
                statFieldName.endsWith("Seconds") -> getTimeString(value)
                else -> value.toString()
            })
        }
        fields["payload"] = sb.toString()

        return fields
    }

    private val uptimeUnits = mapOf(
        24 * 60 * 60 to "d",
        60 * 60 to "h",
        60 to "m"
    )

    private fun getTimeString(uptimeSeconds: Long): String {
        val sb = StringBuilder()
        var seconds = uptimeSeconds

        for ((unitSize, letter) in uptimeUnits) {
            if (seconds >= unitSize) {
                sb.append(seconds / unitSize).append(letter)
                seconds = seconds.rem(unitSize)
            }
        }

        if (sb.isEmpty() || seconds > 0) {
            sb.append(seconds).append("s")
        }

        return sb.toString()
    }

    private val memoryUnits = mapOf(
        1024 * 1024 * 1024 to "G",
        1024 * 1024 to "M",
        1024 to "K"
    )

    private fun getBytesString(bytesNum: Long): String {
        for ((unitSize, letter) in memoryUnits) {
            if (bytesNum >= unitSize) {
                val n = bytesNum.toFloat() / unitSize
                return String.format(if (n > 99) "%.0f" else "%.1f", n).replace(Regex("\\.0$"), "") + letter
            }
        }
        return bytesNum.toString()
    }

    companion object {
        private val uptimeDate = Date()

        fun start() {
            // noop, but it will initialize the uptime date
        }
    }
}
