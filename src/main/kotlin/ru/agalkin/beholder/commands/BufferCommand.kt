package ru.agalkin.beholder.commands

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.config.expressions.Arguments
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.LeafCommandAbstract
import ru.agalkin.beholder.conveyor.Conveyor
import ru.agalkin.beholder.queue.DEFAULT_DATA_BUFFER_COMPRESSION
import ru.agalkin.beholder.queue.DEFAULT_DATA_BUFFER_SIZE
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class BufferCommand(app: Beholder, arguments: Arguments) : CommandAbstract(app, arguments) {
    private val memoryBytes     = AtomicInteger(DEFAULT_DATA_BUFFER_SIZE)
    private val compressionName = AtomicReference<ConfigOption.Compression>(DEFAULT_DATA_BUFFER_COMPRESSION)

    init {
        // buffer { ... }
        arguments.end()
    }

    override fun createSubcommand(args: Arguments): CommandAbstract? {
        return when (args.getCommandName()) {
            "memory_bytes" -> BufferOptionCommand(app, args) {
                val definition = args.shiftFixedString("An integer option value is required")
                memoryBytes.set(ConfigOption.intFromString(definition))
            }
            "memory_compression" -> BufferOptionCommand(app, args) {
                val definition = args.shiftFixedString("Compression mode name is required")
                compressionName.set(ConfigOption.compressionFromString(definition))
            }
            else -> null
        }
    }

    override fun start() {
        app.defaultBuffer.setMemoryBytes(memoryBytes.get())

        val newCompressionName = compressionName.get()
        if (newCompressionName != null) {
            app.defaultBuffer.setCompression(newCompressionName)
        }
    }

    override fun buildConveyor(conveyor: Conveyor): Conveyor {
        return conveyor
    }

    private class BufferOptionCommand(app: Beholder, arguments: Arguments, valueCallback: () -> Unit) : LeafCommandAbstract(app, arguments) {
        init {
            valueCallback()
            arguments.end()
        }

        override fun buildConveyor(conveyor: Conveyor)
            = conveyor
    }
}
