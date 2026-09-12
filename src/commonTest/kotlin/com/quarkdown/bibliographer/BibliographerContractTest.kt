package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.token.toPlainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The cross-platform contract of the [Bibliographer] factory: these tests run
 * on every target, so any behavioral drift between the platform backends
 * fails the build rather than surfacing to consumers.
 */
class BibliographerContractTest {
    private val bibtex =
        """
        @article{einstein1905,
            author = {Einstein, Albert},
            title = {Zur Elektrodynamik bewegter K{\"o}rper},
            journal = {Annalen der Physik},
            year = {1905}
        }
        """.trimIndent()

    private fun source(content: String = bibtex) = BibliographySource(content, BibliographyFormat.BIBTEX)

    @Test
    fun `catalog styles resolve by name`() {
        val bibliographer = Bibliographer(style = "ieee", source = source())

        assertEquals(listOf("einstein1905"), bibliographer.citationKeys)
        assertEquals("[1]", bibliographer.citation("einstein1905")?.toPlainText())
    }

    @Test
    fun `every catalog style resolves on this platform`() {
        StyleCatalog.names.forEach { name ->
            val bibliographer = Bibliographer(style = name, source = source())
            assertEquals(listOf("einstein1905"), bibliographer.citationKeys, "style: $name")
        }
    }

    @Test
    fun `names outside the catalog are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Bibliographer(style = "not-an-existing-style", source = source())
        }
    }

    @Test
    fun `invalid sources are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Bibliographer(style = "ieee", source = source("{{{"))
        }
    }

    @Test
    fun `sources with no entries are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Bibliographer(style = "ieee", source = source("% no entries here"))
        }
    }
}
