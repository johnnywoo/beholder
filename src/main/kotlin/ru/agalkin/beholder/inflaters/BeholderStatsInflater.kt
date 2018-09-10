package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.stats.Stats
import java.time.ZonedDateTime

class BeholderStatsInflater : InplaceInflater {
    private val statsHolder = Stats.createHolder()

    fun stop() {
        Stats.removeHolder(statsHolder)
    }

    override fun inflateMessageFieldsInplace(message: Message): Boolean {
        val influxSb = StringBuilder()
        influxSb.append("beholder")
        for (fieldName in message.getFieldNames().sorted()) {
            val value = message.getStringField(fieldName)
            if (value.contains(' ')) {
                continue
            }
            influxSb.append(",$fieldName=$value")
        }
        influxSb.append(' ')

        val statValues = statsHolder.getStatValuesAndReset().toSortedMap()

        val payloadSb = StringBuilder()
        for ((statFieldName, value) in statValues) {
            message[statFieldName] = value.toString()

            if (!payloadSb.isEmpty()) {
                payloadSb.append(' ')
                influxSb.append(',')
            }
            payloadSb.append(
                statFieldName
                    .replace("(Bytes|Seconds)$".toRegex(), "")
                    .replace("[A-Z]".toRegex()) { "-" + it.groups[0]?.value?.toLowerCase() }
            )
            payloadSb.append(' ')
            payloadSb.append(when {
                statFieldName.endsWith("Bytes") -> getBytesString(value)
                statFieldName.endsWith("Seconds") -> getTimeString(value)
                else -> value.toString()
            })

            influxSb.append(statFieldName).append('=').append(value.toString())
        }

        val date = ZonedDateTime.now()
        influxSb.append(' ').append((date.toEpochSecond() * 1_000_000_000 + date.nano).toString())

        message["payload"] = payloadSb.toString()
        message["influxLineProtocolPayload"] = influxSb.toString()

        return true
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
}
