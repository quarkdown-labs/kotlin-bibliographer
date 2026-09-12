package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.hayagriva.HayagrivaBibliographer

public actual fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String?,
): Bibliographer = HayagrivaBibliographer(style, source, locale)
