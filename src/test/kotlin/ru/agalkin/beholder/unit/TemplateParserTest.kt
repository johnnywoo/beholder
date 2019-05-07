package ru.agalkin.beholder.unit

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import ru.agalkin.beholder.config.TemplateParser
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.testutils.TestInputProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateParserTest {
    private class GoodTemplateParserProvider : TestInputProvider() {
        init {
            // Поскольку в Котлине нет вменяемых строковых литералов без эскейпинга,
            // мы тут обалденно везде меняем ¥ на $ и / на \
            good( "text", "text")
            good( "¥var", "", "var")
            good( "a¥var!", "a", "var", "!")
            good( "a¥var", "a", "var")
            good( "¥var!", "", "var", "!")
            good( "/¥var", "¥var")
            good( "//¥var", "/", "var")
            good( "///¥var", "/¥var")
            good( "////¥var", "//", "var")
            good( "¥a", "", "a")
            good( "x¥a!", "x", "a", "!")
            good( "¥a!", "", "a", "!")
            good( "{¥var}", "", "var")
            good( "a{¥var}b", "a", "var", "b")
            good( "{¥var}b", "", "var", "b")
            good( "¥a ¥b", "", "a", " ", "b")
            good( "{¥a} ¥b", "", "a", " ", "b")
            good( "¥a {¥b}", "", "a", " ", "b")
            good( "a/{¥b}c", "a{", "b", "}c")
            good( "a{b}c", "a{b}c")
            good( "a{b¥c}d", "a{b", "c", "}d")
        }

        private fun good(template: String, vararg result: String)
            = case(decrap(template), result.map(Companion::decrap))
    }

    private class BadTemplateParserProvider : TestInputProvider() {
        init {
            // Поскольку в Котлине нет вменяемых строковых литералов без эскейпинга,
            // мы тут обалденно везде меняем ¥ на $ и / на \
            bad("¥!", "Char '!' (offset 1) is illegal as field name start: ¥!")
            bad("{¥!}", "Char '!' (offset 2) is illegal as field name start: {¥!}")
            bad("{¥}", "Char '}' (offset 2) is illegal as field name start: {¥}")
            bad("{a¥}", "Char '}' (offset 3) is illegal as field name start: {a¥}")
        }

        private fun bad(template: String, result: String)
            = case(decrap(template), decrap(result))
    }

    @ParameterizedTest
    @ArgumentsSource(GoodTemplateParserProvider::class)
    fun runGoodTemplatesTest(template: String, result: List<String>) {
        assertEquals(
            result,
            TemplateParser.parse(template, false, true).parts,
            "Invalid parsing of: $template"
        )
    }

    @ParameterizedTest
    @ArgumentsSource(BadTemplateParserProvider::class)
    fun runBadTemplatesTest(template: String, message: String) {
        try {
            TemplateParser.parse(template, false, true)
            assertTrue(false, "This should not have parsed correctly: $template")
        } catch (e: ParseException) {
            assertEquals(message, e.message)
        }
    }

    companion object {
        private fun decrap(string: String)
            = string.replace('¥', '$').replace('/', '\\')
    }
}
