package com.quarkdown.bibliographer

/**
 * Creates a [Bibliographer] backed by the current platform's CSL processor.
 * @param style the CSL style: either a style name resolvable by the platform's
 *              processor (e.g. `"ieee"`) or the XML content of a style definition
 * @param source the bibliography source to render entries from
 * @param locale optional [RFC 4646](https://www.rfc-editor.org/rfc/rfc4646) locale tag
 *               (e.g. `"en-US"`, `"de-DE"`), controlling localized terms.
 *               When `null`, the style's default locale is used
 * @return a new [Bibliographer] over the source's entries
 * @throws IllegalArgumentException if the style or source cannot be parsed,
 *                                  the source format is unsupported by this platform's backend,
 *                                  or the source contains no entries
 */
public expect fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String? = null,
): Bibliographer
