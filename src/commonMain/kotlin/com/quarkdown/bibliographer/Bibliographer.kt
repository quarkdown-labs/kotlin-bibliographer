package com.quarkdown.bibliographer

import com.quarkdown.bibliographer.token.BibliographyToken

/**
 * The library's main entry point: renders citations and bibliographies
 * for a fixed bibliography source, style and locale,
 * delegating processing to a platform-specific CSL processor, thread-safely.
 */
public interface Bibliographer {
    /**
     * The citation keys of all entries, in bibliography source order.
     * Note that [bibliography] entries may follow a different order,
     * as defined by the style's sorting rules.
     */
    public val citationKeys: List<String>

    /**
     * Formats all bibliography entries, in the order dictated by the style.
     *
     * This is a potentially expensive operation, as it processes every entry at once,
     * although implementations may cache the result.
     *
     * @return the formatted bibliography entries
     */
    public fun bibliography(): List<FormattedEntry>

    /**
     * Formats the combined in-text citation for one or more entries
     * (e.g. `[1]`, `[1, 2]`, or `(Einstein, 1905; Hawking, 1988)`).
     *
     * @param citationKeys the keys of the cited entries
     * @return the formatted citation, or `null` if the processor produced no output
     */
    public fun citation(citationKeys: List<String>): List<BibliographyToken>?
}

/**
 * Formats the in-text citation for a single entry.
 * @param citationKey the key of the cited entry
 * @return the formatted citation, or `null` if the processor produced no output
 * @see Bibliographer.citation
 */
public fun Bibliographer.citation(citationKey: String): List<BibliographyToken>? = citation(listOf(citationKey))
