package ru.agalkin.beholder.config

import ru.agalkin.beholder.config.expressions.CommandException

enum class ConfigOption(val defaultValue: Any, val type: Type) {
    QUEUE_CHUNK_MESSAGES(500, Type.INT),
    BUFFER_MEMORY_BYTES(128 * 1024 * 1024, Type.INT),
    BUFFER_COMPRESSION(Compression.LZ4_FAST, Type.COMPRESSION),

    EXTRA_GC_INTERVAL_SECONDS(5, Type.INT);

    enum class Type {
        INT,
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
    }
}
