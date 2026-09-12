@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.quarkdown.bibliographer.hayagriva

import com.quarkdown.bibliographer.token.BibliographyToken

/**
 * Parses the wrapper crate's token JSON (see `hayagriva-wasm/src/tokens.rs`)
 * into [BibliographyToken]s.
 */
internal object TokenJsonParser {
    private val decorators: Map<String, (BibliographyToken) -> BibliographyToken> =
        mapOf(
            "italic" to BibliographyToken::Italic,
            "oblique" to BibliographyToken::Oblique,
            "bold" to BibliographyToken::Bold,
            "light" to BibliographyToken::Light,
            "small-caps" to BibliographyToken::SmallCaps,
            "underline" to BibliographyToken::Underline,
            "sup" to BibliographyToken::Superscript,
            "sub" to BibliographyToken::Subscript,
        )

    fun parseTokens(json: String): List<BibliographyToken> {
        // The engine signals an empty render as an empty string.
        if (json.isEmpty()) return emptyList()
        return parseJsonArrayOrNull(json)?.let(::parseTokensArray).orEmpty()
    }

    fun parseTokensArray(array: JsAny): List<BibliographyToken> = array.mapElements(::parseToken)

    private fun parseToken(obj: JsAny): BibliographyToken =
        when (val kind = stringProperty(obj, "kind")) {
            "text" -> {
                BibliographyToken.Text(stringProperty(obj, "text").orEmpty())
            }

            "link" -> {
                val url = stringProperty(obj, "url").orEmpty()
                when (val label = objectProperty(obj, "label")) {
                    null -> BibliographyToken.Link(url)
                    else -> BibliographyToken.Link(url, parseToken(label))
                }
            }

            else -> {
                val decorator =
                    requireNotNull(decorators[kind]) { "Unknown token kind: $kind" }
                val child =
                    requireNotNull(objectProperty(obj, "child")) { "Decorator without child: $kind" }
                decorator(parseToken(child))
            }
        }
}
