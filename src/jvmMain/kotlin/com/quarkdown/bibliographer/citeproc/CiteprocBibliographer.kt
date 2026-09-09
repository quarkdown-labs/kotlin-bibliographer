package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.Bibliographer
import com.quarkdown.bibliographer.FormattedEntry
import com.quarkdown.bibliographer.citeproc.CiteprocBibliographer.Companion.from
import com.quarkdown.bibliographer.token.BibliographyToken
import de.undercouch.citeproc.BibliographyFileReader
import de.undercouch.citeproc.CSL
import de.undercouch.citeproc.ItemDataProvider
import java.io.InputStream

/**
 * A [Bibliographer] powered by [citeproc-java](https://github.com/michel-kraemer/citeproc-java).
 *
 * This enables support for any [CSL](https://citationstyles.org) style definition,
 * with bibliography sources in BibTeX, CSL JSON, YAML, EndNote and RIS formats
 * (see the [from] factory).
 *
 * @param style the CSL style: either a style name resolvable from the classpath (e.g. `"ieee"`)
 *              or the serialized XML content of a style definition
 * @param provider the item data provider supplying bibliography data to citeproc-java
 * @param locale optional [RFC 4646](https://www.rfc-editor.org/rfc/rfc4646) locale tag
 *               (e.g. `"en-US"`, `"de-DE"`), controlling localized terms.
 *               When `null`, the style's default locale is used, falling back to `"en-US"`
 */
public class CiteprocBibliographer(
    style: String,
    provider: ItemDataProvider,
    locale: String? = null,
) : Bibliographer {
    private val format = TokenCollectingFormat()

    private val csl =
        CSL(provider, style, locale).apply {
            setOutputFormat(format)
            registerCitationItems(provider.ids)
        }

    override val citationKeys: List<String> = provider.ids.toList()

    /**
     * Lazily formatted entries: triggering this value calls [CSL.makeBibliography],
     * which invokes [TokenCollectingFormat.doFormatBibliographyEntry] for each entry
     * sequentially. The accumulated results are then matched to [citationKeys] by position.
     */
    private val formattedEntries: List<FormattedEntry> by lazy {
        format.collectedEntries.clear()
        csl.makeBibliography()
        citationKeys.zip(format.collectedEntries) { citationKey, (label, content) ->
            FormattedEntry(citationKey, label, content)
        }
    }

    override fun bibliography(): List<FormattedEntry> = formattedEntries

    override fun citation(citationKeys: List<String>): List<BibliographyToken>? {
        val knownKeys = citationKeys.filter(this.citationKeys.toSet()::contains)
        if (knownKeys.isEmpty()) return null

        csl.makeCitation(*knownKeys.toTypedArray())
        return format.lastCitationResult.takeIf { it.isNotEmpty() }
    }

    public companion object {
        /**
         * Creates a [CiteprocBibliographer] by reading a bibliography source file.
         * Supports BibTeX (`.bib`), CSL JSON, YAML, EndNote and RIS formats.
         * @param style the CSL style name or serialized XML content
         * @param input the input stream of the bibliography source
         * @param filename the filename hint for format detection
         * @param locale optional RFC 4646 locale tag
         * @return a new [CiteprocBibliographer] over the file's entries
         */
        public fun from(
            style: String,
            input: InputStream,
            filename: String,
            locale: String? = null,
        ): CiteprocBibliographer {
            val provider = BibliographyFileReader().readBibliographyFile(input, filename)
            return CiteprocBibliographer(style, provider, locale)
        }
    }
}
