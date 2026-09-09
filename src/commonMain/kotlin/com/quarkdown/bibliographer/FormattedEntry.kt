package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.token.BibliographyToken

/**
 * A fully formatted bibliography entry produced by a CSL processor.
 * @param citationKey the unique key identifying the entry in the bibliography source
 * @param label the list label preceding the entry, as plain text
 *              (e.g. `[1]` for numbered styles such as IEEE),
 *              or `null` for styles without labels (e.g. APA)
 * @param content the formatted entry content
 */
public data class FormattedEntry(
    val citationKey: String,
    val label: String?,
    val content: List<BibliographyToken>,
)
