package com.quarkdown.bibliographer.token

/**
 * Formatting applied to a [BibliographyToken.Text] run,
 * mirroring the formatting attributes defined by the
 * [CSL specification](https://docs.citationstyles.org/en/stable/specification.html#formatting).
 */
public data class TextFormat(
    val italic: Boolean = false,
    val bold: Boolean = false,
    val smallCaps: Boolean = false,
) {
    /**
     * Whether no formatting is applied.
     */
    public val isPlain: Boolean
        get() = this == Plain

    public companion object {
        /**
         * The absence of formatting.
         */
        public val Plain: TextFormat = TextFormat()
    }
}
