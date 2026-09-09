package com.quarkdown.bibliographer.token

/**
 * Extracts the plain text of this token, discarding formatting and link targets.
 */
public fun BibliographyToken.toPlainText(): String =
    when (this) {
        is BibliographyToken.Text -> text
        is BibliographyToken.Link -> label.toPlainText()
        is BibliographyToken.Formatted -> token.toPlainText()
    }

/**
 * Extracts the joined plain text of these tokens, discarding formatting and link targets.
 */
public fun List<BibliographyToken>.toPlainText(): String = joinToString("") { it.toPlainText() }
