package ru.agalkin.beholder

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import ru.agalkin.beholder.testutils.TestAbstract
import ru.agalkin.beholder.testutils.TestInputProvider
import kotlin.test.assertEquals

class BasenameFormatterTest : TestAbstract() {
    private class BasenameProvider : TestInputProvider() {
        init {
            case("..", "noname")
            case(".", "noname")
            case("/", "noname")
            case("~", "noname")
            case("|", "noname")

            case("a/b/c", "c")
            case("c", "c")
            case("..a", "..a")
            case("a/b/c/", "c")
            case("~user", "user")
            case("/path/filename.ext", "filename.ext")
            case("beholder-collector", "beholder-collector")
        }
    }

    @ParameterizedTest
    @ArgumentsSource(BasenameProvider::class)
    fun runTest(path: String, expected: String) {
        val message = Message.of("path" to path)

        val parsedMessage = processMessageWithConfig(message, "set \$basename basename \$path")

        assertEquals(expected, parsedMessage?.getStringField("basename"))
    }
}
