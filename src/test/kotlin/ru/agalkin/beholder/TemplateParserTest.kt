package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.TemplateParser
import ru.agalkin.beholder.config.parser.ParseException
import kotlin.test.assertEquals

class TemplateParserTest : TestAbstract() {
    // Поскольку в Котлине нет вменяемых стороковых литералов без эскейпинга,
    // мы тут обалденно везде меняем ¥ на $ и / на \
    @Test fun testJsonParses()   { assertTemplateParses("text", "text") }
    @Test fun testJsonParses2()  { assertTemplateParses("¥var", "", "var") }
    @Test fun testJsonParses3()  { assertTemplateParses("a¥var!", "a", "var", "!") }
    @Test fun testJsonParses4()  { assertTemplateParses("a¥var", "a", "var") }
    @Test fun testJsonParses5()  { assertTemplateParses("¥var!", "", "var", "!") }
    @Test fun testJsonParses6()  { assertTemplateParses("/¥var", "¥var") }
    @Test fun testJsonParses7()  { assertTemplateParses("//¥var", "/", "var") }
    @Test fun testJsonParses8()  { assertTemplateParses("///¥var", "/¥var") }
    @Test fun testJsonParses9()  { assertTemplateParses("////¥var", "//", "var") }
    @Test fun testJsonParses10() { assertTemplateParses("¥a", "", "a") }
    @Test fun testJsonParses11() { assertTemplateParses("x¥a!", "x", "a", "!") }
    @Test fun testJsonParses12() { assertTemplateParses("¥a!", "", "a", "!") }
    @Test fun testJsonParses13() { assertTemplateParses("{¥var}", "", "var") }
    @Test fun testJsonParses14() { assertTemplateParses("a{¥var}b", "a", "var", "b") }
    @Test fun testJsonParses15() { assertTemplateParses("{¥var}b", "", "var", "b") }
    @Test fun testJsonParses16() { assertTemplateParses("¥a ¥b", "", "a", " ", "b") }
    @Test fun testJsonParses17() { assertTemplateParses("{¥a} ¥b", "", "a", " ", "b") }
    @Test fun testJsonParses18() { assertTemplateParses("¥a {¥b}", "", "a", " ", "b") }
    @Test fun testJsonParses19() { assertTemplateParses("a/{¥b}c", "a{", "b", "}c") }
    @Test fun testJsonParses20() { assertTemplateParses("a{b}c", "a{b}c") }
    @Test fun testJsonParses21() { assertTemplateParses("a{b¥c}d", "a{b", "c", "}d") }

    @Test fun testJsonFails1()  { assertTemplateFails("¥!", "Char '!' (offset 1) is illegal as field name start: ¥!") }
    @Test fun testJsonFails2()  { assertTemplateFails("{¥!}", "Char '!' (offset 2) is illegal as field name start: {¥!}") }
    @Test fun testJsonFails3()  { assertTemplateFails("{¥}", "Char '}' (offset 2) is illegal as field name start: {¥}") }
    @Test fun testJsonFails4()  { assertTemplateFails("{a¥}", "Char '}' (offset 3) is illegal as field name start: {a¥}") }

    private fun assertTemplateParses(template: String, vararg result: String) {
        val normalTemplate = template.replace('¥', '$').replace('/', '\\')
        assertEquals(
            result.toList().map { it.replace('¥', '$').replace('/', '\\') },
            TemplateParser.parse(normalTemplate, false, true).parts,
            "Invalid parsing of: $normalTemplate"
        )
    }

    private fun assertTemplateFails(template: String, message: String) {
        val normalTemplate = template.replace('¥', '$').replace('/', '\\')
        try {
            TemplateParser.parse(normalTemplate, false, true)
            assertEquals(false, true, "This should not have parsed correctly: $normalTemplate")
        } catch (e: ParseException) {
            assertEquals(message.replace('¥', '$'), e.message)
        }
    }
}
