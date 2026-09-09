package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.token.BibliographyToken.Bold
import com.quarkdown.bibliographer.token.BibliographyToken.Italic
import com.quarkdown.bibliographer.token.BibliographyToken.Link
import com.quarkdown.bibliographer.token.BibliographyToken.SmallCaps
import com.quarkdown.bibliographer.token.BibliographyToken.Text
import de.undercouch.citeproc.csl.internal.TokenBuffer
import de.undercouch.citeproc.csl.internal.behavior.FormattingAttributes
import de.undercouch.citeproc.csl.internal.token.TextToken
import kotlin.test.Test
import kotlin.test.assertEquals

class CiteprocTokenConverterTest {
    private val converter =
        CiteprocTokenConverter { text, type ->
            when (type) {
                TextToken.Type.DOI -> "https://doi.org/$text"
                else -> text
            }
        }

    private fun buffer(vararg tokens: TextToken): TokenBuffer =
        TokenBuffer().apply {
            tokens.forEach(::append)
        }

    @Test
    fun `converts formatted text tokens`() {
        val buffer =
            buffer(
                TextToken("Relativity", TextToken.Type.TEXT, FormattingAttributes.ofFontStyle(FormattingAttributes.FS_ITALIC)),
                TextToken(", ", TextToken.Type.DELIMITER),
                TextToken("Einstein", TextToken.Type.TEXT, FormattingAttributes.ofFontWeight(FormattingAttributes.FW_BOLD)),
            )

        assertEquals(
            listOf(
                Italic(Text("Relativity")),
                Text(", "),
                Bold(Text("Einstein")),
            ),
            converter.convert(buffer),
        )
    }

    @Test
    fun `nests decorators innermost-first`() {
        val attributes =
            FormattingAttributes.merge(
                FormattingAttributes.merge(
                    FormattingAttributes.ofFontStyle(FormattingAttributes.FS_ITALIC),
                    FormattingAttributes.ofFontWeight(FormattingAttributes.FW_BOLD),
                ),
                FormattingAttributes.ofFontVariant(FormattingAttributes.FV_SMALLCAPS),
            )
        val buffer = buffer(TextToken("Everything", TextToken.Type.TEXT, attributes))

        assertEquals(
            listOf(SmallCaps(Bold(Italic(Text("Everything"))))),
            converter.convert(buffer),
        )
    }

    @Test
    fun `resolves DOI tokens into links`() {
        val buffer = buffer(TextToken("10.1000/xyz123", TextToken.Type.DOI))

        assertEquals(
            listOf(Link("https://doi.org/10.1000/xyz123")),
            converter.convert(buffer),
        )
    }

    @Test
    fun `link labels preserve formatting`() {
        val buffer =
            buffer(
                TextToken("https://example.com", TextToken.Type.URL, FormattingAttributes.ofFontStyle(FormattingAttributes.FS_ITALIC)),
            )

        assertEquals(
            listOf(
                Link(
                    url = "https://example.com",
                    label = Italic(Text("https://example.com")),
                ),
            ),
            converter.convert(buffer),
        )
    }

    @Test
    fun `skips empty tokens`() {
        val buffer =
            buffer(
                TextToken("", TextToken.Type.TEXT),
                TextToken("content", TextToken.Type.TEXT),
            )

        assertEquals(
            listOf(Text("content")),
            converter.convert(buffer),
        )
    }

    @Test
    fun `extracts plain text discarding formatting`() {
        val buffer =
            buffer(
                TextToken("[", TextToken.Type.PREFIX),
                TextToken("1", TextToken.Type.TEXT, FormattingAttributes.ofFontWeight(FormattingAttributes.FW_BOLD)),
                TextToken("]", TextToken.Type.SUFFIX),
            )

        assertEquals("[1]", converter.extractPlainText(buffer))
    }
}
