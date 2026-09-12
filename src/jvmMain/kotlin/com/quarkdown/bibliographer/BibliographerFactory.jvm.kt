package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.citeproc.CiteprocBibliographer
import java.io.IOException

public actual fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String?,
): Bibliographer {
    val bibliographer =
        try {
            CiteprocBibliographer.from(
                style = style,
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
