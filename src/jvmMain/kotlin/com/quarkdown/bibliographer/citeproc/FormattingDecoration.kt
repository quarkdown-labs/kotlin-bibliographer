package com.quarkdown.bibliographer.citeproc

import com.quarkdown.bibliographer.token.BibliographyToken
import com.quarkdown.bibliographer.token.BibliographyToken.Bold
import com.quarkdown.bibliographer.token.BibliographyToken.Italic
import com.quarkdown.bibliographer.token.BibliographyToken.Light
import com.quarkdown.bibliographer.token.BibliographyToken.Oblique
import com.quarkdown.bibliographer.token.BibliographyToken.SmallCaps
import com.quarkdown.bibliographer.token.BibliographyToken.Subscript
import com.quarkdown.bibliographer.token.BibliographyToken.Superscript
import com.quarkdown.bibliographer.token.BibliographyToken.Underline
import de.undercouch.citeproc.csl.internal.behavior.FormattingAttributes

/**
 * A single citeproc-java formatting attribute, associated with
 * the [BibliographyToken.Formatted] decorator it maps to.
 * @param attribute extracts this attribute's group from the packed bitmask
 * @param value the group value that enables the decorator
 * @param decorator wraps a token with the corresponding formatting
 */
internal class FormattingDecoration(
    private val attribute: (Int) -> Int,
    private val value: Int,
    private val decorator: (BibliographyToken) -> BibliographyToken.Formatted,
) {
    /**
     * @return [token] wrapped by this decoration if [attributes] enables it, [token] itself otherwise
     */
    fun applyTo(
        token: BibliographyToken,
        attributes: Int,
    ): BibliographyToken = if (attribute(attributes) == value) decorator(token) else token
}

/**
 * All supported formatting decorations, applied innermost-first:
 * font style, weight, variant, text decoration, vertical alignment.
 */
private val decorations: List<FormattingDecoration> =
    listOf(
        FormattingDecoration(FormattingAttributes::getFontStyle, FormattingAttributes.FS_ITALIC, ::Italic),
        FormattingDecoration(FormattingAttributes::getFontStyle, FormattingAttributes.FS_OBLIQUE, ::Oblique),
        FormattingDecoration(FormattingAttributes::getFontWeight, FormattingAttributes.FW_BOLD, ::Bold),
        FormattingDecoration(FormattingAttributes::getFontWeight, FormattingAttributes.FW_LIGHT, ::Light),
        FormattingDecoration(FormattingAttributes::getFontVariant, FormattingAttributes.FV_SMALLCAPS, ::SmallCaps),
        FormattingDecoration(FormattingAttributes::getTextDecoration, FormattingAttributes.TD_UNDERLINE, ::Underline),
        FormattingDecoration(FormattingAttributes::getVerticalAlign, FormattingAttributes.VA_SUP, ::Superscript),
        FormattingDecoration(FormattingAttributes::getVerticalAlign, FormattingAttributes.VA_SUB, ::Subscript),
    )

/**
 * Wraps this token in the [BibliographyToken.Formatted] decorators
 * enabled by a citeproc-java [FormattingAttributes] bitmask.
 */
internal fun BibliographyToken.decoratedWith(attributes: Int): BibliographyToken =
    decorations.fold(this) { token, decoration -> decoration.applyTo(token, attributes) }
