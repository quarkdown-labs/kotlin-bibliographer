package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.token.toPlainText
import kotlin.test.Test
import kotlin.test.assertEquals

class BibliographerFactoryTest {
    private val bibtex =
        """
        @article{einstein1905,
            author = {Einstein, Albert},
            title = {Zur Elektrodynamik bewegter K{\"o}rper},
            journal = {Annalen der Physik},
            year = {1905}
        }
        """.trimIndent()

    @Test
    fun `creates a platform bibliographer from a common source`() {
        val bibliographer =
            Bibliographer(
                style = "ieee",
                source = BibliographySource(bibtex, BibliographyFormat.BIBTEX),
            )

        assertEquals(listOf("einstein1905"), bibliographer.citationKeys)
        assertEquals("[1]", bibliographer.citation("einstein1905")?.toPlainText())
    }
}
