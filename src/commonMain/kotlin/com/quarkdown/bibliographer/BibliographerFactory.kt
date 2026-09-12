package com.quarkdown.bibliographer

/**
 * Creates a [Bibliographer] backed by the current platform's CSL processor.
 * @param style the CSL style: either a name from [StyleCatalog] (e.g. `"ieee"`)
 *              or the XML content of a style definition
 * @param source the bibliography source to render entries from
 * @param locale optional [RFC 4646](https://www.rfc-editor.org/rfc/rfc4646) locale tag
 *               (e.g. `"en-US"`, `"de-DE"`), controlling localized terms.
 *               When `null`, the style's default locale is used
 * @return a new [Bibliographer] over the source's entries
 * @throws IllegalArgumentException if the style name is not in [StyleCatalog],
 *                                  the style or source cannot be parsed,
 *                                  the source format is unsupported by this platform's backend,
 *                                  or the source contains no entries
 */
public expect fun Bibliographer(
    style: String,
    source: BibliographySource,
    locale: String? = null,
): Bibliographer

/**
 * Validates the platform factories' [style] argument: either CSL XML content,
 * or a name from [StyleCatalog]: names outside the catalog are rejected on
 * every platform, even when a platform's processor could resolve them, so
 * that a name accepted on one platform is accepted on all of them.
 * @throws IllegalArgumentException if [style] is a name outside the catalog
 */
internal fun requireCatalogStyle(style: String) {
    require(style.trimStart().startsWith("<") || style in StyleCatalog.names) {
        "Unknown style name: \"$style\". " +
            "Pass one of the ${StyleCatalog.names.size} names in StyleCatalog, " +
            "or the XML content of a CSL style definition."
    }
}
