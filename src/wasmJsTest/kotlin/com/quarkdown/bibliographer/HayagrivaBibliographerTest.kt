package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.token.toPlainText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HayagrivaBibliographerTest {
    private val bibtex =
        """
        @article{einstein1905,
            author = {Einstein, Albert},
            title = {Zur Elektrodynamik bewegter K{\"o}rper},
            journal = {Annalen der Physik},
            year = {1905}
        }
        @book{hawking1988,
            author = {Hawking, Stephen},
            title = {A Brief History of Time},
            year = {1988}
        }
        """.trimIndent()

    private fun bibliographer(style: String): Bibliographer = Bibliographer(style, BibliographySource(bibtex, BibliographyFormat.BIBTEX))

    @Test
    fun `citation keys are exposed in source order`() {
        assertEquals(listOf("einstein1905", "hawking1988"), bibliographer(IEEE_CSL).citationKeys)
    }

    @Test
    fun `numbered style produces labeled citations and entries`() {
        val bibliographer = bibliographer(IEEE_CSL)

        assertEquals("[1]", bibliographer.citation("einstein1905")?.toPlainText())
        assertEquals("[1], [2]", bibliographer.citation(listOf("einstein1905", "hawking1988"))?.toPlainText())

        val bibliography = bibliographer.bibliography()
        assertEquals(listOf("einstein1905", "hawking1988"), bibliography.map { it.citationKey })
        assertEquals(listOf("[1]", "[2]"), bibliography.map { it.label })
        assertContains(bibliography[0].content.toPlainText(), "Einstein")
    }

    @Test
    fun `author-date style produces unlabeled entries`() {
        val bibliographer = bibliographer(APA_CSL)

        assertEquals("(Einstein, 1905)", bibliographer.citation("einstein1905")?.toPlainText())

        val entry = bibliographer.bibliography().first { it.citationKey == "hawking1988" }
        assertNull(entry.label)
        assertContains(entry.content.toPlainText(), "Hawking")
    }

    @Test
    fun `unknown citation keys produce no output`() {
        assertNull(bibliographer(IEEE_CSL).citation("unknown"))
    }

    @Test
    fun `unknown citation keys are ignored among known ones`() {
        assertEquals("[1]", bibliographer(IEEE_CSL).citation(listOf("einstein1905", "unknown"))?.toPlainText())
    }

    @Test
    fun `csl json sources are supported`() {
        val json =
            """[{"id":"doe2000","type":"book","title":"T","author":[{"family":"Doe","given":"J"}],"issued":{"date-parts":[[2000]]}}]"""
        val bibliographer = Bibliographer(IEEE_CSL, BibliographySource(json, BibliographyFormat.CSL_JSON))
        assertEquals("[1]", bibliographer.citation("doe2000")?.toPlainText())
    }

    @Test
    fun `citation keys containing backslashes round-trip through citation`() {
        val json =
            """[{"id":"back\\slash","type":"book","title":"T","author":[{"family":"Doe","given":"J"}],"issued":{"date-parts":[[2000]]}}]"""
        val bibliographer = Bibliographer(IEEE_CSL, BibliographySource(json, BibliographyFormat.CSL_JSON))
        assertEquals(listOf("back\\slash"), bibliographer.citationKeys)
        assertEquals("[1]", bibliographer.citation("back\\slash")?.toPlainText())
    }

    @Test
    fun `unsupported source formats are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Bibliographer(IEEE_CSL, BibliographySource("TY  - BOOK", BibliographyFormat.RIS))
        }
    }

    @Test
    fun `locale override selects localized terms`() {
        // IEEE renders the page label from the locale's terms: "pp." in English,
        // "S." in German (mirrors the Rust `locale_override_selects_localized_terms` test).
        val paged =
            """
            @article{einstein1905,
                author = {Einstein, Albert},
                title = {Zur Elektrodynamik bewegter K{\"o}rper},
                journal = {Annalen der Physik},
                pages = {891--921},
                year = {1905}
            }
            """.trimIndent()
        val bibliographer =
            Bibliographer(IEEE_CSL, BibliographySource(paged, BibliographyFormat.BIBTEX), locale = "de-DE")

        assertContains(
            bibliographer
                .bibliography()
                .first()
                .content
                .toPlainText(),
            "S.",
        )
    }
}
