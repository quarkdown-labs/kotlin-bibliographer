package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.token.BibliographyToken
import de.undercouch.citeproc.csl.internal.RenderContext
import de.undercouch.citeproc.csl.internal.SBibliography
import de.undercouch.citeproc.csl.internal.TokenBuffer
import de.undercouch.citeproc.csl.internal.format.BaseFormat
import de.undercouch.citeproc.csl.internal.token.TextToken
import de.undercouch.citeproc.output.Bibliography
import de.undercouch.citeproc.output.SecondFieldAlign

/**
 * A bibliography entry tokenized by [TokenCollectingFormat],
 * not yet associated with its citation key.
 * @param label the list label as plain text (e.g. `[1]`), or `null` when the style defines none
 * @param content the formatted entry content
 */
internal data class TokenizedEntry(
    val label: String?,
    val content: List<BibliographyToken>,
)

/**
 * A custom citeproc-java [BaseFormat] that produces [BibliographyToken]s.
 *
 * Since citeproc-java's API is string-based, the `doFormat*` callbacks return empty strings
 * while storing structured results in side-channel fields.
 *
 * Token conversion is delegated to [CiteprocTokenConverter].
 */
internal class TokenCollectingFormat : BaseFormat() {
    private val converter =
        CiteprocTokenConverter { text, type ->
            when (type) {
                TextToken.Type.DOI -> addDOIPrefix(text)
                else -> formatURL(text)
            }
        }

    /**
     * Accumulated tokenized bibliography entries, populated sequentially
     * during [de.undercouch.citeproc.CSL.makeBibliography] calls.
     */
    val collectedEntries: MutableList<TokenizedEntry> = mutableListOf()

    /**
     * The most recent citation result, set by [doFormatCitation]
     * and to be consumed by the caller immediately after.
     */
    var lastCitationResult: List<BibliographyToken> = emptyList()
        private set

    override fun getName(): String = "bibliographer"

    override fun doFormatCitation(
        buffer: TokenBuffer,
        renderContext: RenderContext,
    ): String {
        lastCitationResult = converter.convert(buffer)
        return ""
    }

    override fun doFormatBibliographyEntry(
        buffer: TokenBuffer,
        renderContext: RenderContext,
        index: Int,
    ): String {
        collectedEntries += tokenizeEntry(buffer, renderContext)
        return ""
    }

    // Links are handled directly in CiteprocTokenConverter.
    override fun doFormatLink(
        text: String,
        uri: String,
    ): String = text

    override fun makeBibliography(
        entries: Array<out String>,
        bibliography: SBibliography,
    ): Bibliography = Bibliography(*entries)

    /**
     * Tokenizes a single bibliography entry, splitting it into a label and content when the style uses
     * [second-field-align](https://docs.citationstyles.org/en/stable/specification.html#bibliography-specific-options).
     *
     * Styles with `second-field-align` (e.g. IEEE) split the token buffer into:
     * - **First-field tokens**: the entry label (e.g. `[1]`), extracted as plain text
     * - **Remaining tokens**: the entry content
     *
     * Styles without it (e.g. APA) treat the entire buffer as content, with no label.
     */
    private fun tokenizeEntry(
        buffer: TokenBuffer,
        renderContext: RenderContext,
    ): TokenizedEntry {
        val secondFieldAlign = renderContext.style.bibliography?.secondFieldAlign

        if (secondFieldAlign == null || secondFieldAlign == SecondFieldAlign.FALSE) {
            return TokenizedEntry(label = null, content = converter.convert(buffer))
        }

        val tokens = buffer.tokens
        val contentStart = tokens.indexOfFirst { !it.isFirstField }

        if (contentStart <= 0) {
            return TokenizedEntry(label = null, content = converter.convert(buffer))
        }

        val label = converter.extractPlainText(buffer.copy(0, contentStart))
        val content = converter.convert(buffer.copy(contentStart, tokens.size))
        return TokenizedEntry(label, content)
    }
}
