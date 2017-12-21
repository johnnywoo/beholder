package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message

class BeholderStatsInflater : Inflater {
    override fun inflateMessageFields(message: Message) {
        val runtime = Runtime.getRuntime()

        val heapSize   = runtime.totalMemory()
        val heapMax    = runtime.maxMemory()
        val heapUnused = runtime.freeMemory()

        val heapUsed = heapSize - heapUnused

        val uptimeDate = Beholder.uptimeDate
        val uptimeSeconds = when (uptimeDate) {
            null -> 0
            else -> ((System.currentTimeMillis() - uptimeDate.time) / 1000).toInt()
        }

        message["uptimeSeconds"] = uptimeSeconds.toString()
        message["heapBytes"]     = heapSize.toString()
        message["heapUsedBytes"] = heapUsed.toString()
        message["heapMaxBytes"]  = heapMax.toString()
        message["payload"]       = "heap ${getMemoryString(heapSize)} heap-used ${getMemoryString(heapUsed)} heap-max ${getMemoryString(heapMax)} uptime ${getUptimeString(uptimeSeconds)}"
    }

    private val uptimeUnits = mapOf(
        24 * 60 * 60 to "d",
        60 * 60 to "h",
        60 to "m"
    )

    private fun getUptimeString(uptimeSeconds: Int): String {
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

    private fun getMemoryString(bytesNum: Long): String {
        for ((unitSize, letter) in memoryUnits) {
            if (bytesNum >= unitSize) {
                val n = bytesNum.toFloat() / unitSize
                return String.format(if (n > 99) "%.0f" else "%.1f", n).replace(Regex("\\.0$"), "") + letter
            }
        }
        return bytesNum.toString()
    }
}
