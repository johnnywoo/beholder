package ru.agalkin.beholder.passthrough

import ru.agalkin.beholder.Message
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
        assertNotNull(
            processMessageWithConfig(Message(), "keep ¥payload")
        )
    }

    @Test
    fun testDropWorksReal() {
        // drop = no message gets out
        assertNull(
            processMessageWithConfig(Message(), "drop")
        )
    }
}
