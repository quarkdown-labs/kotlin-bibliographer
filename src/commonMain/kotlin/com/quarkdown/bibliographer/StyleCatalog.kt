package com.quarkdown.bibliographer

/**
 * The catalog of CSL styles that every platform backend resolves by name,
 * rendering from the same style definition on each platform.
 *
 * Each name is the style's [Zotero](https://www.zotero.org/styles) id.
 * Every platform artifact embeds the same style definitions, vendored in the
 * repository's `styles/` directory, so a catalog name resolves to identical
 * XML everywhere with no extra dependency.
 *
 * Styles outside the catalog can always be used by passing the XML content
 * of their definition to [Bibliographer] instead of a name.
 */
public object StyleCatalog {
    /** The Zotero ids of the styles resolvable by name on every platform. */
    public val names: Set<String> =
        setOf(
            "american-anthropological-association",
            "american-chemical-society",
            "american-geophysical-union",
            "american-institute-of-aeronautics-and-astronautics",
            "american-institute-of-physics",
            "american-medical-association",
            "american-meteorological-society",
            "american-physics-society",
            "american-physiological-society",
            "american-political-science-association",
            "american-society-for-microbiology",
            "american-society-of-civil-engineers",
            "american-society-of-mechanical-engineers",
            "american-sociological-association",
            "angewandte-chemie",
            "annual-reviews",
            "annual-reviews-author-date",
            "apa",
            "associacao-brasileira-de-normas-tecnicas",
            "association-for-computing-machinery",
            "biomed-central",
            "bmj",
            "bristol-university-press",
            "cell",
            "chicago-author-date",
            "chicago-notes",
            "chicago-notes-bibliography",
            "chicago-shortened-notes-bibliography",
            "copernicus-publications",
            "current-opinion",
            "deutsche-gesellschaft-fur-psychologie",
            "deutsche-sprache",
            "elsevier-harvard",
            "elsevier-vancouver",
            "elsevier-with-titles",
            "frontiers",
            "future-medicine",
            "future-science-group",
            "gost-r-7-0-5-2008-numeric",
            "harvard-cite-them-right",
            "ieee",
            "institute-of-physics-numeric",
            "karger-journals",
            "mary-ann-liebert-vancouver",
            "modern-language-association",
            "multidisciplinary-digital-publishing-institute",
            "nature",
            "pensoft-journals",
            "plos",
            "royal-society-of-chemistry",
            "sage-vancouver",
            "sist02",
            "spie-journals",
            "springer-basic-author-date",
            "springer-basic-brackets",
            "springer-fachzeitschriften-medizin-psychologie",
            "springer-humanities-author-date",
            "springer-lecture-notes-in-computer-science",
            "springer-mathphys-brackets",
            "springer-socpsych-author-date",
            "springer-vancouver",
            "taylor-and-francis-chicago-author-date",
            "taylor-and-francis-national-library-of-medicine",
            "the-institution-of-engineering-and-technology",
            "the-lancet",
            "thieme-german",
            "trends-journals",
        )
}
