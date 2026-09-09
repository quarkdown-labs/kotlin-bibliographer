package com.quarkdown.bibliographer.token

/**
 * A fragment of formatted bibliographic content produced by a CSL processor,
 * such as a citation label or a bibliography entry.
 *
 * Formatting is expressed by decorated composition.
 */
public sealed interface BibliographyToken {
    /**
     * A plain run of text.
     * @param text the text content
     */
    public data class Text(
        val text: String,
    ) : BibliographyToken

    /**
     * A hyperlink, such as a URL or a resolved DOI.
     * @param url the target URL
     * @param label the display content, possibly [Formatted], defaulting to the plain URL itself
     */
    public data class Link(
        val url: String,
        val label: BibliographyToken = Text(url),
    ) : BibliographyToken

    /**
     * Decorator that applies formatting to the [token] it wraps, mirroring the
     * formatting attributes defined by the
     * [CSL specification](https://docs.citationstyles.org/en/stable/specification.html#formatting).
     */
    public sealed interface Formatted : BibliographyToken {
        /**
         * The wrapped token the formatting applies to.
         */
        public val token: BibliographyToken
    }

    /**
     * Italic formatting (CSL `font-style: italic`).
     */
    public data class Italic(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Oblique formatting (CSL `font-style: oblique`).
     */
    public data class Oblique(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Bold formatting (CSL `font-weight: bold`).
     */
    public data class Bold(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Light formatting (CSL `font-weight: light`).
     */
    public data class Light(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Small capitals formatting (CSL `font-variant: small-caps`).
     */
    public data class SmallCaps(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Underline formatting (CSL `text-decoration: underline`).
     */
    public data class Underline(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Superscript formatting (CSL `vertical-align: sup`).
     */
    public data class Superscript(
        override val token: BibliographyToken,
    ) : Formatted

    /**
     * Subscript formatting (CSL `vertical-align: sub`).
     */
    public data class Subscript(
        override val token: BibliographyToken,
    ) : Formatted
}
