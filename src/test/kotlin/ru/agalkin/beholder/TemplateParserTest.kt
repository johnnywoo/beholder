package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.formatters.TemplateFormatter
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

    @Test fun testJsonFails1()  { assertTemplateFails("¥!", "Char '!' (offset 1) is illegal as field name start: ¥!") }
    @Test fun testJsonFails2()  { assertTemplateFails("{¥!}", "Char '!' (offset 2) is illegal as field name start: {¥!}") }
    @Test fun testJsonFails3()  { assertTemplateFails("{¥}", "Char '}' (offset 2) is illegal as field name start: {¥}") }

    private fun assertTemplateParses(template: String, vararg result: String) {
        val normalTemplate = template.replace('¥', '$').replace('/', '\\')
        assertEquals(
            result.toList().map { it.replace('¥', '$').replace('/', '\\') },
            TemplateFormatter.TemplateParser.parse(normalTemplate, false),
            "Invalid parsing of: $normalTemplate"
        )
    }

    private fun assertTemplateFails(template: String, message: String) {
        val normalTemplate = template.replace('¥', '$').replace('/', '\\')
        try {
            TemplateFormatter.TemplateParser.parse(normalTemplate, false)
            assertEquals(false, true, "This should not have parsed correctly: $normalTemplate")
        } catch (e: ParseException) {
            assertEquals(message.replace('¥', '$'), e.message)
        }
    }
}
