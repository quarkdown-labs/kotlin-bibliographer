package com.quarkdown.bibliographer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibliographyFormatTest {
    @Test
    fun `infers format from filename extension`() {
        assertEquals(BibliographyFormat.BIBTEX, BibliographyFormat.fromFilename("references.bib"))
        assertEquals(BibliographyFormat.CSL_JSON, BibliographyFormat.fromFilename("refs.json"))
        assertEquals(BibliographyFormat.RIS, BibliographyFormat.fromFilename("export.ris"))
    }

    @Test
    fun `inference is case-insensitive and alias-aware`() {
        assertEquals(BibliographyFormat.BIBTEX, BibliographyFormat.fromFilename("REFERENCES.BIB"))
        assertEquals(BibliographyFormat.YAML, BibliographyFormat.fromFilename("refs.yaml"))
        assertEquals(BibliographyFormat.YAML, BibliographyFormat.fromFilename("refs.yml"))
    }

    @Test
    fun `unknown or missing extensions produce no format`() {
        assertNull(BibliographyFormat.fromFilename("references.txt"))
        assertNull(BibliographyFormat.fromFilename("references"))
        assertNull(BibliographyFormat.fromFilename("bib"))
    }
}
