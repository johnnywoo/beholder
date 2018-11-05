package ru.agalkin.beholder.config

import ru.agalkin.beholder.config.expressions.CommandException
import java.time.DateTimeException
import java.time.ZoneId
import java.time.zone.ZoneRulesException

enum class ConfigOption(val defaultValue: Any, val type: Type) {
    QUEUE_CHUNK_MESSAGES(500, Type.INT),

    CREATE_DATES_IN_TIMEZONE(ZoneId.systemDefault(), Type.TIMEZONE);

    enum class Type {
        INT,
        TIMEZONE,
        COMPRESSION
    }

    enum class Compression {
        OFF,
        LZ4_FAST
    }

    companion object {
        fun intFromString(string: String): Int {
            val match = "^(\\d+)([kmg])?$".toRegex(RegexOption.IGNORE_CASE).matchEntire(string)
            val n = match?.groups?.get(1)?.value?.toIntOrNull()
            if (n == null) {
                throw CommandException("An integer option value is required")
            }
            return n * when (match.groups[2]?.value?.toLowerCase()) {
                "g" -> 1024 * 1024 * 1024
                "m" -> 1024 * 1024
                "k" -> 1024
                else -> 1
            }
        }

        fun compressionFromString(string: String): Compression {
            return when (string.toLowerCase()) {
                "off" -> Compression.OFF
                "lz4-fast" -> Compression.LZ4_FAST
                else -> throw CommandException("Unknown compression mode: $string")
            }
        }

        fun timezoneFromString(string: String): ZoneId {
            try {
                return ZoneId.of(string)
            } catch (e: DateTimeException) {
                throw CommandException("Invalid timezone: $string")
            } catch (e: ZoneRulesException) {
                throw CommandException("Unknown timezone: $string")
            }
        }
    }
}
