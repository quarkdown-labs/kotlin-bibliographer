package com.quarkdown.bibliographer.hayagriva

import com.quarkdown.bibliographer.token.BibliographyToken.Bold
import com.quarkdown.bibliographer.token.BibliographyToken.Italic
import com.quarkdown.bibliographer.token.BibliographyToken.Link
import com.quarkdown.bibliographer.token.BibliographyToken.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class TokenJsonParserTest {
    @Test
    fun `parses text tokens`() {
        assertEquals(
            listOf(Text("Hello")),
            TokenJsonParser.parseTokens("""[{"kind":"text","text":"Hello"}]"""),
        )
    }

    @Test
    fun `parses nested decorators innermost-first`() {
        assertEquals(
            listOf(Bold(Italic(Text("x")))),
            TokenJsonParser.parseTokens(
                """[{"kind":"bold","child":{"kind":"italic","child":{"kind":"text","text":"x"}}}]""",
            ),
        )
    }

    @Test
    fun `parses links with decorated labels`() {
        assertEquals(
            listOf(Link(url = "https://doi.org/10.1/x", label = Italic(Text("10.1/x")))),
            TokenJsonParser.parseTokens(
                """[{"kind":"link","url":"https://doi.org/10.1/x",""" +
                    """"label":{"kind":"italic","child":{"kind":"text","text":"10.1/x"}}}]""",
            ),
        )
    }

    @Test
    fun `empty input produces no tokens`() {
        assertEquals(emptyList(), TokenJsonParser.parseTokens(""))
        assertEquals(emptyList(), TokenJsonParser.parseTokens("[]"))
    }
}
