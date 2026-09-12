@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.quarkdown.bibliographer.hayagriva

import com.quarkdown.bibliographer.Bibliographer
import com.quarkdown.bibliographer.BibliographyFormat
import com.quarkdown.bibliographer.BibliographySource
import com.quarkdown.bibliographer.FormattedEntry
import com.quarkdown.bibliographer.knownCitationKeys
import com.quarkdown.bibliographer.token.BibliographyToken

/**
 * A [Bibliographer] powered by [hayagriva](https://github.com/typst/hayagriva),
 * compiled to WebAssembly and vendored with this library — no external
 * dependencies are required at build or run time.
 *
 * Current limitations of this backend:
 * - Only [BibliographyFormat.BIBTEX] and [BibliographyFormat.CSL_JSON] sources are supported.
 *
 * wasmJs (and JavaScript in general) runs single-threaded, so [Bibliographer]'s
 * "thread-safely" contract is trivially satisfied here — there is no concurrent access to guard against.
 *
 * @param style the XML content of a CSL style definition, or the Zotero id of a style
 *              embedded in the binding's archive (a superset of `StyleCatalog`;
 *              the platform factory restricts names to the catalog)
 * @param source the bibliography source
 * @param locale optional [RFC 4646](https://www.rfc-editor.org/rfc/rfc4646) locale tag.
 *               When `null`, the style's default locale is used
 * @throws IllegalArgumentException if the source format is unsupported,
 *                                  the style name is unknown,
 *                                  or the style or source cannot be parsed
 */
public class HayagrivaBibliographer(
    style: String,
    source: BibliographySource,
    locale: String? = null,
) : Bibliographer {
    private val engine: HayagrivaEngine

    override val citationKeys: List<String>

    private val registeredKeys: Set<String>

    init {
        val format =
            when (source.format) {
                BibliographyFormat.BIBTEX -> "bibtex"

                BibliographyFormat.CSL_JSON -> "csl-json"

                else -> throw IllegalArgumentException(
                    "The hayagriva backend does not support ${source.format} bibliography sources.",
                )
            }

        engine =
            try {
                HayagrivaEngine(style, source.content, format, locale)
            } catch (e: JsException) {
                throw IllegalArgumentException("The bibliography could not be created: ${e.message}", e)
            }

        citationKeys = parseStringArray(engine.citation_keys())
        registeredKeys = citationKeys.toSet()
    }

    /**
     * Lazily formatted entries: triggering this value calls [HayagrivaEngine.bibliography],
     * which formats every entry in the source at once, in the order dictated
     * by the style's sorting rules.
     */
    private val formattedEntries: List<FormattedEntry> by lazy {
        parseJsonArrayOrNull(engine.bibliography())
            ?.mapElements { item ->
                FormattedEntry(
                    citationKey = stringProperty(item, "citationKey").orEmpty(),
                    label = stringProperty(item, "label"),
                    content =
                        objectProperty(item, "content")
                            ?.let(TokenJsonParser::parseTokensArray)
                            .orEmpty(),
                )
            }.orEmpty()
    }

    override fun bibliography(): List<FormattedEntry> = formattedEntries

    override fun citation(citationKeys: List<String>): List<BibliographyToken>? {
        val knownKeys = knownCitationKeys(citationKeys, registeredKeys) ?: return null

        val keysJson = stringsToJson(knownKeys)
        return TokenJsonParser
            .parseTokens(engine.citation(keysJson))
            .takeIf { it.isNotEmpty() }
    }

    private fun parseStringArray(json: String): List<String> = parseJsonArrayOrNull(json)?.mapElements(::elementToString).orEmpty()
}
