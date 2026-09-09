package com.quarkdown.bibliographer

/**
 * A bibliography data format.
 * @param extension the canonical file extension for the format, without the leading dot
 * @param aliasExtensions alternative file extensions the format is also known by
 */
public enum class BibliographyFormat(
    public val extension: String,
    private vararg val aliasExtensions: String,
) {
    BIBTEX("bib"),
    CSL_JSON("json"),
    YAML("yaml", "yml"),
    ENDNOTE("enl"),
    RIS("ris"),
    ;

    /**
     * @return whether this format is known by the given file [extension], case-insensitively
     */
    private fun matches(extension: String): Boolean =
        this.extension.equals(extension, ignoreCase = true) ||
            aliasExtensions.any { it.equals(extension, ignoreCase = true) }

    public companion object {
        /**
         * Infers the format of a bibliography source from its file name.
         * @param filename the file name, whose extension determines the format (e.g. `references.bib`)
         * @return the matching format, or `null` if the extension is unknown
         */
        public fun fromFilename(filename: String): BibliographyFormat? {
            val extension = filename.substringAfterLast('.', missingDelimiterValue = "")
            return entries.firstOrNull { it.matches(extension) }
        }
    }
}

/**
 * The textual content of a bibliography source, such as a `.bib` file.
 * @param content the raw source content
 * @param format the format of the content
 */
public data class BibliographySource(
    val content: String,
    val format: BibliographyFormat,
)
