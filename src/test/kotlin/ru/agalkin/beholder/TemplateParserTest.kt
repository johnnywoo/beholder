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

    @Test fun testJsonFails1()  { assertTemplateFails("¥!", "Char '!' (offset 1) is illegal as field name start: ¥!") }

    private fun assertTemplateParses(template: String, vararg result: String) {
        assertEquals(
            result.toList().map { it.replace('¥', '$').replace('/', '\\') },
            TemplateFormatter.TemplateParser.parse(template.replace('¥', '$').replace('/', '\\'))
        )
    }

    private fun assertTemplateFails(template: String, message: String) {
        try {
            TemplateFormatter.TemplateParser.parse(template.replace('¥', '$').replace('/', '\\'))
            assertEquals(false, true, "This should not have parsed correctly: $template")
        } catch (e: ParseException) {
            assertEquals(message.replace('¥', '$'), e.message)
        }
    }
}
