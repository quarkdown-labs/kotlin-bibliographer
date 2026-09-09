package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.token.BibliographyToken
import com.quarkdown.bibliographer.token.BibliographyToken.Link
import com.quarkdown.bibliographer.token.BibliographyToken.Text
import de.undercouch.citeproc.csl.internal.TokenBuffer
import de.undercouch.citeproc.csl.internal.token.TextToken

/**
 * Converts [citeproc-java](https://github.com/michel-kraemer/citeproc-java)
 * [TokenBuffer] tokens into platform-agnostic [BibliographyToken]s.
 *
 * This is the core of the JVM bridge: formatting attributes map to
 * [BibliographyToken.Formatted] decorators, and URL/DOI tokens map to [Link]s.
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
                Link(url = url, label = Text(url).decoratedWith(token.formattingAttributes))
            }

            else -> {
                Text(token.text).decoratedWith(token.formattingAttributes)
            }
        }
}
