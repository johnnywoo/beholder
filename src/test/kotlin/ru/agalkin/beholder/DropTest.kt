package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DropTest : TestAbstract() {
    @Test
    fun testDropParses() {
        assertConfigParses("drop", "drop;\n")
    }

    @Test
    fun testDropWorks() {
        // no drop = message gets out
        val message = Message()
        val processedMessage = processMessageWithConfig(message, "keep \$payload")
        assertNotNull(processedMessage)

        // drop = no message gets out
        val message2 = Message()
        val processedMessage2 = processMessageWithConfig(message2, "drop")
        assertNull(processedMessage2)
    }
}
