package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.citeproc.CiteprocBibliographer
import java.io.IOException

public actual fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String?,
): Bibliographer {
    requireCatalogStyle(style)
    val styleXml = if (style.trimStart().startsWith("<")) style else catalogStyleXml(style)
    val bibliographer =
        try {
            CiteprocBibliographer.from(
                style = styleXml,
                input = source.content.byteInputStream(),
                filename = "source.${source.format.extension}",
                locale = locale,
            )
        } catch (e: IOException) {
            throw IllegalArgumentException("The bibliography could not be created: ${e.message}", e)
        }

    require(bibliographer.citationKeys.isNotEmpty()) {
        "The bibliography could not be created: the ${source.format} source contains no entries."
    }
    return bibliographer
}

/**
 * The XML content of a [StyleCatalog] style, embedded in this artifact, so name
 * resolution needs no styles dependency on the consumer's classpath.
 */
private fun catalogStyleXml(name: String): String =
    checkNotNull(
        CiteprocBibliographer::class.java.getResourceAsStream("/com/quarkdown/bibliographer/styles/$name.csl"),
    ) { "Catalog style \"$name\" is not embedded in the artifact." }
        .reader()
        .use { it.readText() }
