package ru.agalkin.beholder

import org.junit.jupiter.api.assertThrows
import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import java.lang.Exception
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockTest : NetworkedTestAbstract() {
    @Test
    fun testMockTrivial() {
        val config = """from mock; to mock"""
        makeAppAndMock(config) { _, mock ->
            mock.send("cat" to "feline")
            mock.receive()
        }
    }

    @Test
    fun testMockMoreCommands() {
        val config = """from mock; set ¥payload canine; to mock"""
        makeAppAndMock(config) { _, mock ->
            mock.send()
            val fieldValue = mock.receive()
            assertEquals("canine", fieldValue.toString())
        }
    }

    @Test
    fun testMockUnreceived() {
        val exception = assertThrows<Exception> {
            val config = """from mock; to mock"""
            makeAppAndMock(config) { _, mock ->
                mock.send()
            }
        }
        assertEquals("Mock received too many messages: expected 0, actual 1", exception.message)
    }

    @Test
    fun testMockSmoke() {
        val message = Message.of("cat" to "feline")

        val config = """from mock; to mock"""
        makeApp(config).use { app ->
            app.config.start()

            app.mockListeners["default"]!!.queue.add(message)

            Thread.sleep(50)
            assertTrue { app.mockSenders["default"]!!.received.size == 1 }
        }
    }
}
