package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class BasenameFormatterTest : TestAbstract() {
    @Test fun testBasenameBad1() = runTest("..", "noname")
    @Test fun testBasenameBad2() = runTest(".", "noname")
    @Test fun testBasenameBad3() = runTest("/", "noname")
    @Test fun testBasenameBad4() = runTest("~", "noname")
    @Test fun testBasenameBad5() = runTest("|", "noname")

    @Test fun testBasename1() = runTest("a/b/c", "c")
    @Test fun testBasename2() = runTest("c", "c")
    @Test fun testBasename3() = runTest("..a", "..a")
    @Test fun testBasename4() = runTest("a/b/c/", "c")
    @Test fun testBasename5() = runTest("~user", "user")
    @Test fun testBasename6() = runTest("/path/filename.ext", "filename.ext")

    private fun runTest(path: String, expected: String) {
        val message = Message()
        message["path"] = path

        val parsedMessage = processMessageWithCommand(message, "set \$basename basename \$path")

        assertEquals(expected, parsedMessage?.getStringField("basename"))
    }
}
