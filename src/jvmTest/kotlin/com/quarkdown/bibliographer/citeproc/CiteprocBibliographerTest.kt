package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.citation
import com.quarkdown.bibliographer.token.toPlainText
import de.undercouch.citeproc.ListItemDataProvider
import de.undercouch.citeproc.csl.CSLItemData
import de.undercouch.citeproc.csl.CSLItemDataBuilder
import de.undercouch.citeproc.csl.CSLType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CiteprocBibliographerTest {
    private val einstein: CSLItemData =
        CSLItemDataBuilder()
            .id("einstein1905")
            .type(CSLType.ARTICLE_JOURNAL)
            .title("Zur Elektrodynamik bewegter Körper")
            .author("Albert", "Einstein")
            .issued(1905)
            .containerTitle("Annalen der Physik")
            .build()

    private val hawking: CSLItemData =
        CSLItemDataBuilder()
            .id("hawking1988")
            .type(CSLType.BOOK)
            .title("A Brief History of Time")
            .author("Stephen", "Hawking")
            .issued(1988)
            .build()

    private fun bibliographer(style: String): CiteprocBibliographer = CiteprocBibliographer(style, ListItemDataProvider(einstein, hawking))

    @Test
    fun `citation keys are exposed in source order`() {
        assertEquals(
            listOf("einstein1905", "hawking1988"),
            bibliographer("ieee").citationKeys,
        )
    }

    @Test
    fun `numbered style produces labeled citations and entries`() {
        val bibliographer = bibliographer("ieee")

        assertEquals("[1]", bibliographer.citation("einstein1905")?.toPlainText())
        assertEquals("[2]", bibliographer.citation("hawking1988")?.toPlainText())
        assertEquals("[1], [2]", bibliographer.citation(listOf("einstein1905", "hawking1988"))?.toPlainText())

        val bibliography = bibliographer.bibliography()
        assertEquals(listOf("einstein1905", "hawking1988"), bibliography.map { it.citationKey })
        assertEquals(listOf("[1]", "[2]"), bibliography.map { it.label })
        assertContains(bibliography[0].content.toPlainText(), "Einstein")
        assertContains(bibliography[1].content.toPlainText(), "Hawking")
    }

    @Test
    fun `author-date style produces unlabeled entries`() {
        val bibliographer = bibliographer("apa")

        assertEquals("(Einstein, 1905)", bibliographer.citation("einstein1905")?.toPlainText())

        val entry = bibliographer.bibliography().first { it.citationKey == "hawking1988" }
        assertNull(entry.label)
        assertContains(entry.content.toPlainText(), "Hawking")
    }

    @Test
    fun `unknown citation keys produce no output`() {
        assertNull(bibliographer("ieee").citation("unknown"))
    }

    @Test
    fun `unknown citation keys are ignored among known ones`() {
        assertEquals("[1]", bibliographer("ieee").citation(listOf("einstein1905", "unknown"))?.toPlainText())
    }
}
