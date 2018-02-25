package ru.agalkin.beholder

enum class ConfigOption(val defaultValue: Any, val type: Type) {
    FROM_INTERNAL_LOG_BUFFER_MESSAGES_COUNT(1000, Type.INT),
    FROM_TCP_BUFFER_MESSAGES_COUNT(1000, Type.INT),
    FROM_UDP_BUFFER_MESSAGES_COUNT(1000, Type.INT),

    TO_FILE_BUFFER_MESSAGES_COUNT(1000, Type.INT),
    TO_SHELL_BUFFER_MESSAGES_COUNT(1000, Type.INT),
    TO_TCP_BUFFER_MESSAGES_COUNT(1000, Type.INT),
    TO_UDP_BUFFER_MESSAGES_COUNT(1000, Type.INT);

    enum class Type {
        INT
    }
}
