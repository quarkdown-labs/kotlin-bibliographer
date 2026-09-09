package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.token.BibliographyToken
import com.quarkdown.bibliographer.token.TextFormat
import de.undercouch.citeproc.csl.internal.TokenBuffer
import de.undercouch.citeproc.csl.internal.behavior.FormattingAttributes
import de.undercouch.citeproc.csl.internal.token.TextToken

/**
 * Converts [citeproc-java](https://github.com/michel-kraemer/citeproc-java)
 * [TokenBuffer] tokens into platform-agnostic [BibliographyToken]s.
 *
 * This is the core of the JVM bridge: formatting attributes (italic, bold, small caps)
 * map to [TextFormat], and URL/DOI tokens map to [BibliographyToken.Link].
 *
 * @param urlResolver resolves raw URL/DOI text into target URLs
 *                    (e.g. prepending `https://doi.org/` to DOIs)
 */
public class CiteprocTokenConverter(
    private val urlResolver: UrlResolver = UrlResolver { text, _ -> text },
) {
    /**
     * Resolves raw URL/DOI text into target URLs.
     */
    public fun interface UrlResolver {
        /**
         * @param text the raw URL or DOI text
         * @param type the token type ([TextToken.Type.URL] or [TextToken.Type.DOI])
         * @return the resolved URL
         */
        public fun resolve(
            text: String,
            type: TextToken.Type,
        ): String
    }

    /**
     * Converts all tokens in a [TokenBuffer] into [BibliographyToken]s.
     *
     * Non-text tokens (e.g. display groups) are skipped, as layout is the consumer's concern.
     */
    public fun convert(buffer: TokenBuffer): List<BibliographyToken> =
        buffer.tokens
            .asSequence()
            .filterIsInstance<TextToken>()
            .filter { it.text.isNotEmpty() }
            .map(::convert)
            .toList()

    /**
     * Extracts plain text from a [TokenBuffer], discarding formatting.
     */
    public fun extractPlainText(buffer: TokenBuffer): String =
        buffer.tokens
            .filterIsInstance<TextToken>()
            .joinToString("") { it.text }
            .trim()

    private fun convert(token: TextToken): BibliographyToken =
        when (token.type) {
            TextToken.Type.URL, TextToken.Type.DOI -> {
                val url = urlResolver.resolve(token.text, token.type)
                BibliographyToken.Link(
                    url = url,
                    label = BibliographyToken.Text(url, token.formattingAttributes.toTextFormat()),
                )
            }

            else -> {
                BibliographyToken.Text(token.text, token.formattingAttributes.toTextFormat())
            }
        }
}

/**
 * Maps a citeproc-java [FormattingAttributes] bitmask to a [TextFormat].
 */
private fun Int.toTextFormat(): TextFormat =
    TextFormat(
        italic = FormattingAttributes.getFontStyle(this) == FormattingAttributes.FS_ITALIC,
        bold = FormattingAttributes.getFontWeight(this) == FormattingAttributes.FW_BOLD,
        smallCaps = FormattingAttributes.getFontVariant(this) == FormattingAttributes.FV_SMALLCAPS,
    )
