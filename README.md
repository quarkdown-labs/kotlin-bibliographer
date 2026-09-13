# kotlin-bibliographer

A Kotlin Multiplatform bridge from [Citation Style Language](https://citationstyles.org) (`.csl`) processors
to a platform-agnostic bibliography **token domain**.

| :x: What this library is ***not*** | :white_check_mark: What it ***is***                |
|:-----------------------------------|:---------------------------------------------------|
| Processor implementation           | Platform-agnostic bridge to established processors |
| CSL &rarr; HTML converter          | CSL &rarr; your AST                                |

## Targets

| Target | Backend                                                                             | Formats                              | Status       |
|--------|-------------------------------------------------------------------------------------|--------------------------------------|--------------|
| JVM    | [citeproc-java](https://github.com/michel-kraemer/citeproc-java) (Java, Apache-2.0) | BibTeX, CSL JSON, YAML, EndNote, RIS | Stable       |
| WASM   | [hayagriva](https://github.com/typst/hayagriva) (Rust, MIT/Apache-2.0)              | BibTeX, CSL JSON                     | Experimental |

## Getting started

```kotlin
dependencies {
    implementation("com.quarkdown.bibliographer:bibliographer:0.5.0")
}
```

```kotlin
val bibliographer =
    Bibliographer(
        style = "ieee",
        source =
            BibliographySource(
                File("references.bib").readText(),
                BibliographyFormat.BIBTEX,
            ),
    )
```

Render in-text citations and the bibliography as tokens:

```kotlin
// [Text("[1]")], or null if the key is unknown.
bibliographer.citation("einstein1905")

bibliographer.bibliography().forEach { entry ->
    entry.citationKey // "einstein1905"
    entry.label       // "[1]" for numbered styles
    entry.content     // [Text("A. Einstein, "), Italic(Text("Zur Elektrodynamik...")), ...]
}
```

### Styles

`style` accepts either a name from `StyleCatalog` (67 popular styles), or the XML content of any CSL style definition.

<details>
<summary>The catalog styles</summary>

- `american-anthropological-association`
- `american-chemical-society`
- `american-geophysical-union`
- `american-institute-of-aeronautics-and-astronautics`
- `american-institute-of-physics`
- `american-medical-association`
- `american-meteorological-society`
- `american-physics-society`
- `american-physiological-society`
- `american-political-science-association`
- `american-society-for-microbiology`
- `american-society-of-civil-engineers`
- `american-society-of-mechanical-engineers`
- `american-sociological-association`
- `angewandte-chemie`
- `annual-reviews`
- `annual-reviews-author-date`
- `apa`
- `associacao-brasileira-de-normas-tecnicas`
- `association-for-computing-machinery`
- `biomed-central`
- `bmj`
- `bristol-university-press`
- `cell`
- `chicago-author-date`
- `chicago-notes`
- `chicago-notes-bibliography`
- `chicago-shortened-notes-bibliography`
- `copernicus-publications`
- `current-opinion`
- `deutsche-gesellschaft-fur-psychologie`
- `deutsche-sprache`
- `elsevier-harvard`
- `elsevier-vancouver`
- `elsevier-with-titles`
- `frontiers`
- `future-medicine`
- `future-science-group`
- `gost-r-7-0-5-2008-numeric`
- `harvard-cite-them-right`
- `ieee`
- `institute-of-physics-numeric`
- `karger-journals`
- `mary-ann-liebert-vancouver`
- `modern-language-association`
- `multidisciplinary-digital-publishing-institute`
- `nature`
- `pensoft-journals`
- `plos`
- `royal-society-of-chemistry`
- `sage-vancouver`
- `sist02`
- `spie-journals`
- `springer-basic-author-date`
- `springer-basic-brackets`
- `springer-fachzeitschriften-medizin-psychologie`
- `springer-humanities-author-date`
- `springer-lecture-notes-in-computer-science`
- `springer-mathphys-brackets`
- `springer-socpsych-author-date`
- `springer-vancouver`
- `taylor-and-francis-chicago-author-date`
- `taylor-and-francis-national-library-of-medicine`
- `the-institution-of-engineering-and-technology`
- `the-lancet`
- `thieme-german`
- `trends-journals`

</details>

Formatting is expressed by decorated composition, and mapping tokens
to your own domain is a recursive match:

```kotlin
fun convert(token: BibliographyToken): MyNode =
    when (token) {
        is Text -> MyText(token.text)
        is Link -> MyLink(url = token.url, label = convert(token.label))
        is Italic -> MyEmphasis(convert(token.token))
        is Bold -> MyStrong(convert(token.token))
        // ...
    }
```

## Licensing notes

Every published artifact embeds the catalog styles from the
[CSL styles project](https://github.com/citation-style-language/styles),
and the WASM artifact additionally embeds locale data from the
[CSL locales project](https://github.com/citation-style-language/locales);
both are licensed under
[CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/).
