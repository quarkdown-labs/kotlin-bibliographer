package com.quarkdown.bibliographer.demo

import com.quarkdown.bibliographer.token.BibliographyToken
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Converts a [BibliographyToken] tree to DOM nodes,
 * mapping each formatting decorator to the matching HTML element.
 */
internal fun BibliographyToken.toNode(): Node =
    when (this) {
        is BibliographyToken.Text -> document.createTextNode(text)
        is BibliographyToken.Link ->
            document.createElement("a").apply {
                setAttribute("href", url)
                appendChild(label.toNode())
            }
        is BibliographyToken.Formatted -> container().apply { appendChild(token.toNode()) }
    }

private fun BibliographyToken.Formatted.container(): Element =
    when (this) {
        is BibliographyToken.Italic -> element("em")
        is BibliographyToken.Oblique -> styledSpan("font-style: oblique")
        is BibliographyToken.Bold -> element("strong")
        is BibliographyToken.Light -> styledSpan("font-weight: lighter")
        is BibliographyToken.SmallCaps -> styledSpan("font-variant: small-caps")
        is BibliographyToken.Underline -> element("u")
        is BibliographyToken.Superscript -> element("sup")
        is BibliographyToken.Subscript -> element("sub")
    }

private fun element(tag: String): Element = document.createElement(tag)

private fun styledSpan(css: String): Element = element("span").apply { setAttribute("style", css) }
