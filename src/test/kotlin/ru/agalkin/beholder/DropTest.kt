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
    fun testDropWorksMeta() {
        // no drop = message gets out
        val message = Message()
        val processedMessage = processMessageWithConfig(message, "keep \$payload")
        assertNotNull(processedMessage)
    }

    @Test
    fun testDropWorksReal() {
        // drop = no message gets out
        val message = Message()
        val processedMessage = processMessageWithConfig(message, "drop")
        assertNull(processedMessage)
    }
}
