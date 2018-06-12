package ru.agalkin.beholder.config

enum class ConfigOption(val defaultValue: Any, val type: Type) {
    QUEUE_CHUNK_MESSAGES(500, Type.INT),

    EXTRA_GC_INTERVAL_SECONDS(5, Type.INT);

    enum class Type {
        INT
    }
}
