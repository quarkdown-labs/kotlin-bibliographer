package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.hayagriva.HayagrivaBibliographer

public actual fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String?,
): Bibliographer {
    requireCatalogStyle(style)
    return HayagrivaBibliographer(style, source, locale)
}
