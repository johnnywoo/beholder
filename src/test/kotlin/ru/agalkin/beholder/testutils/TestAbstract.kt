package ru.agalkin.beholder.testutils

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.conveyor.Step
import ru.agalkin.beholder.conveyor.StepResult
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class TestAbstract {
    protected fun processMessageWithConfig(message: Message, config: String): Message? {
        var processedMessage: Message? = null

        makeApp(config).use { app ->
            val root = app.config.root
            root.start()

            root.topLevelOutput.addStep(conveyorStepOf {
                processedMessage = it
                null
            })

            root.topLevelInput.addMessage(message)
        }

        return processedMessage
    }

    protected fun conveyorStepOf(block: (Message) -> Any?): Step {
        return object : Step {
            override fun execute(message: Message)
                = if (block(message) == StepResult.DROP) StepResult.DROP else StepResult.CONTINUE

            override fun getDescription()
                = "test callback"
        }
    }

    protected fun makeApp(config: String): Beholder {
        return Beholder({
            Config.fromStringWithLog(it, config.replace('¥', '$'), "test-config")
        })
    }

    protected fun getMessageDump(message: Message?) = when (message) {
        null -> "null"
        else -> DumpFormatter().formatMessage(message).toString().replace(Regex("^.*\n"), "")
    }

    protected fun assertConfigParses(fromText: String, toDefinition: String) {
        makeApp("").use { app ->
            assertEquals(toDefinition, Config(app, fromText, "test-config").getDefinition())
        }
    }

    protected fun assertConfigFails(fromText: String, errorMessage: String) {
        try {
            makeApp("").use { app ->
                val definition = Config(app, fromText, "test-config").getDefinition()
                fail("This config should not parse correctly: $fromText\n=== parsed ===\n$definition\n===")
            }
        } catch (e: ParseException) {
            assertEquals(errorMessage, e.message)
        }
    }

    protected fun getByteArrayField(message: Message, field: String): ByteArray {
        val fieldValue = message.getFieldValue(field)
        return fieldValue.toByteArray().slice(0 until fieldValue.getByteLength()).toByteArray()
    }

    @BeforeTest
    fun beforeAllTests() {
        InternalLog.setStdout(null)
        InternalLog.setStderr(null)
    }
}
