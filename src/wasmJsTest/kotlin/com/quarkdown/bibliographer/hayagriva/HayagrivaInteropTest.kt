package com.quarkdown.bibliographer.hayagriva

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HayagrivaInteropTest {
    @Test
    fun `engine loads and renders through the vendored module`() {
        val engine =
            HayagrivaEngine(
                style = MINIMAL_STYLE,
                source = "@book{k1, author={Doe, Jane}, title={T}, year={2000}}",
                format = "bibtex",
                locale = null,
            )

        assertEquals("""["k1"]""", engine.citation_keys())
        assertContains(engine.citation("""["k1"]"""), "text")
    }
}

// A tiny complete CSL style (numeric, no sort) to avoid embedding ieee.csl in this task.
internal val MINIMAL_STYLE: String =
    """
    <?xml version="1.0" encoding="utf-8"?>
    <style xmlns="http://purl.org/net/xbiblio/csl" class="in-text" version="1.0">
      <info><title>Minimal</title><id>minimal</id></info>
      <citation><layout prefix="[" suffix="]"><text variable="citation-number"/></layout></citation>
      <bibliography><layout><text variable="title"/></layout></bibliography>
    </style>
    """.trimIndent()
