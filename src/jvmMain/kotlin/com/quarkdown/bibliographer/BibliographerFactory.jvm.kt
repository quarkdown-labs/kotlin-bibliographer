package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.citeproc.CiteprocBibliographer

public actual fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String?,
): Bibliographer =
    CiteprocBibliographer.from(
        style = style,
        input = source.content.byteInputStream(),
        filename = "source.${source.format.extension}",
        locale = locale,
    )
