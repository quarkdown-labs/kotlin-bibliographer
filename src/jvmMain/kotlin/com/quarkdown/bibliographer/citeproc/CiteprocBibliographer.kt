package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.Bibliographer
import com.quarkdown.bibliographer.FormattedEntry
import com.quarkdown.bibliographer.citeproc.CiteprocBibliographer.Companion.from
import com.quarkdown.bibliographer.knownCitationKeys
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
 * @throws java.io.IOException if the style cannot be loaded or parsed
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

    private val registeredKeys: Set<String> = citationKeys.toSet()

    /**
     * Lazily formatted entries: triggering this value calls [CSL.makeBibliography],
     * which invokes [TokenCollectingFormat.doFormatBibliographyEntry] for each entry
     * sequentially, in the order dictated by the style's sorting rules.
     */
    private val formattedEntries: List<FormattedEntry> by lazy {
        format.collectedEntries.clear()
        csl.makeBibliography()
        format.collectedEntries.toList()
    }

    @Synchronized
    override fun bibliography(): List<FormattedEntry> = formattedEntries

    @Synchronized
    override fun citation(citationKeys: List<String>): List<BibliographyToken>? {
        val knownKeys = knownCitationKeys(citationKeys, registeredKeys) ?: return null

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
         * @throws java.io.IOException if the bibliography source cannot be read or parsed,
         *                             or if the style cannot be loaded or parsed
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
