package com.quarkdown.bibliographer.demo

import com.quarkdown.bibliographer.BibliographyFormat

/**
 * A sample bibliography source per supported format,
 * shown when the format is selected.
 */
internal val SAMPLES: Map<BibliographyFormat, String> =
    mapOf(
        BibliographyFormat.BIBTEX to
            """
            @article{einstein,
              author  = {Albert Einstein},
              title   = {Zur Elektrodynamik bewegter K{\"o}rper},
              journal = {Annalen der Physik},
              volume  = {322},
              number  = {10},
              pages   = {891--921},
              year    = {1905},
              doi     = {10.1002/andp.19053221004}
            }

            @book{hawking,
              author    = {Stephen Hawking},
              title     = {A Brief History of Time},
              publisher = {Bantam Books},
              year      = {1988}
            }
            """.trimIndent(),
        BibliographyFormat.CSL_JSON to
            """
            [
              {
                "id": "einstein",
                "type": "article-journal",
                "author": [{"family": "Einstein", "given": "Albert"}],
                "title": "Zur Elektrodynamik bewegter Körper",
                "container-title": "Annalen der Physik",
                "volume": "322",
                "issue": "10",
                "page": "891-921",
                "issued": {"date-parts": [[1905]]},
                "DOI": "10.1002/andp.19053221004"
              },
              {
                "id": "hawking",
                "type": "book",
                "author": [{"family": "Hawking", "given": "Stephen"}],
                "title": "A Brief History of Time",
                "publisher": "Bantam Books",
                "issued": {"date-parts": [[1988]]}
              }
            ]
            """.trimIndent(),
    )
