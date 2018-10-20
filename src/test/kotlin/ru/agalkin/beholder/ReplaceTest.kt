package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplaceTest : TestAbstract() {
    @Test
    fun testRegexParses() {
        assertConfigParses(
            """
            |set ¥payload replace ~cat~ 'dog';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace ~cat~ 'dog';
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testReplaceBad() {
        assertConfigFails(
            """
            |set ¥payload replace 'cat' 'dog';
            |""".trimMargin().replace('¥', '$'),
            "`replace` needs a regexp: set \$payload replace 'cat' 'dog' [test-config:1]"
        )
    }

    @Test
    fun testReplaceBadRegexModifier() {
        assertConfigFails(
            """
            |set ¥payload replace ~cat~q 'dog';
            |""".trimMargin().replace('¥', '$'),
            "Invalid regexp modifier: q [test-config:1]"
        )
    }

    @Test
    fun testReplaceGoodRegexModifier() {
        assertConfigParses(
            """
            |set ¥payload replace ~cat~i 'dog';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace ~cat~i 'dog';
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testReplaceBadRegexReplacement() {
        assertConfigParses(
            """
            |set ¥payload replace ~cat~ '$1';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace ~cat~ '$1';
            |""".trimMargin().replace('¥', '$')
        )

        val message = Message()
        message["payload"] = "We've got a cat here"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat~ 'huge \$1'")

        // текст не изменился, команда не упала с исключением
        assertEquals("We've got a cat here", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceWorks() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat|dog~ animal")

        assertEquals("We've got animals and animals", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceNewlines() {
        val message = Message()
        message["payload"] = "Line 1\nLine 2"

        val processedMessage = processMessageWithConfig(message, """set ¥payload replace ~\n~ '\\\\n'""".replace('¥', '$'))

        assertEquals("""Line 1\nLine 2""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceInterpolationSimple() {
        val message = Message()
        message["animal"]  = "feline"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat~ '\$animal'")

        assertEquals("""We've got felines and dogs""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceInterpolationAndGroups() {
        val message = Message()
        message["size"]    = "huge"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~(cat|dog)~ '\$size \$1'")

        assertEquals("""We've got huge cats and huge dogs""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceWithEmptyString() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cats and ~ ''")

        assertEquals("""We've got dogs""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceWithUnknownField() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cats and ~ \$unknown")

        assertEquals("""We've got dogs""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceInUnknownField() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cats and ~ '' in \$unknown")

        assertEquals("", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceIn() {
        val message = Message()
        message["payload"] = "To be ignored"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat~ 'animal' in 'Zoo has cats'")

        assertEquals("""Zoo has animals""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceInField() {
        val message = Message()
        message["text"]    = "Zoo has cats"
        message["payload"] = "To be ignored"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat~ 'animal' in \$text")

        assertEquals("""Zoo has animals""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceInWithFields() {
        val message = Message()
        message["animal"]  = "cat"
        message["payload"] = "To be ignored"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat~ 'feline' in 'Two cats: \$animal \$animal'")

        assertEquals("""Two felines: feline feline""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceInWithFieldsEverywhere() {
        val message = Message()
        message["animal"]     = "cat"
        message["betterName"] = "feline"
        message["payload"]    = "To be ignored"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~cat~ \$betterName in 'Two cats: \$animal \$animal'")

        assertEquals("""Two felines: feline feline""", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceFromHelp1() {
        val message = Message()
        message["payload"] = "127.0.0.1 WARN PHP Warning: some warning"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~warn(ing)?~i 'WARNING'")

        assertEquals("127.0.0.1 WARNING PHP WARNING: some WARNING", processedMessage!!.getStringField("payload"))
    }

    @Test
    fun testReplaceFromHelp2() {
        val message = Message()
        message["subdomain"] = "www"
        message["domain"]    = "example.com"

        val processedMessage = processMessageWithConfig(message, "set \$host replace ~^www\\.~ '' in '\$subdomain.\$domain'")

        assertEquals("example.com", processedMessage!!.getStringField("host"))

        val message2 = Message()
        message2["subdomain"] = "mail"
        message2["domain"]    = "example.com"

        val processedMessage2 = processMessageWithConfig(message2, "set \$host replace ~^www\\.~ '' in '\$subdomain.\$domain'")

        assertEquals("mail.example.com", processedMessage2!!.getStringField("host"))
    }

    @Test
    fun testReplaceFromHelp3() {
        val message = Message()
        message["payload"] = "a\nb"

        val processedMessage = processMessageWithConfig(message, "set \$payload replace ~\\n~ '\\\\\\\\n'")

        assertEquals("a\\nb", processedMessage!!.getStringField("payload"))
    }
}
