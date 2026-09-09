package com.quarkdown.bibliographer.token

import com.quarkdown.bibliographer.token.BibliographyToken.Bold
import com.quarkdown.bibliographer.token.BibliographyToken.Italic
import com.quarkdown.bibliographer.token.BibliographyToken.Link
import com.quarkdown.bibliographer.token.BibliographyToken.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class BibliographyTokenTest {
    @Test
    fun `formatting decorators compose`() {
        val token = Bold(Italic(Text("content")))

        assertEquals(Italic(Text("content")), token.token)
        assertEquals(Text("content"), (token.token as Italic).token)
    }

    @Test
    fun `link label defaults to its plain url`() {
        assertEquals(Text("https://example.com"), Link("https://example.com").label)
    }

    @Test
    fun `link label can be formatted`() {
        val link = Link("https://example.com", label = Italic(Text("example")))

        assertEquals(Italic(Text("example")), link.label)
    }
}
