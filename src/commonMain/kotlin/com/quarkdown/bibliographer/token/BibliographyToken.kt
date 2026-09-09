package com.quarkdown.bibliographer.token

/**
 * A fragment of formatted bibliographic content produced by a CSL processor,
 * such as a citation label or a bibliography entry.
 *
 * Tokens are platform-agnostic and carry no layout or markup semantics:
 * consumers map them to their own domain (e.g. an AST, HTML, plain text).
 */
public sealed interface BibliographyToken {
    /**
     * A run of text with uniform [format]ting.
     * @param text the text content
     * @param format the formatting applied to the whole run
     */
    public data class Text(
        val text: String,
        val format: TextFormat = TextFormat.Plain,
    ) : BibliographyToken

    /**
     * A hyperlink, such as a URL or a resolved DOI.
     * @param url the target URL
     * @param label the display text, which may carry its own formatting,
     *              defaulting to the plain URL itself
     */
    public data class Link(
        val url: String,
        val label: Text = Text(url),
    ) : BibliographyToken
}
