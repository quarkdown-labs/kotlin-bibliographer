# kotlin-bibliographer

A Kotlin Multiplatform bridge from [Citation Style Language](https://citationstyles.org) (`.csl`) processors
to a platform-agnostic bibliography **token domain**.

| :x: What this library is ***not*** | :white_check_mark: What it ***is***                |
|:-----------------------------------|:---------------------------------------------------|
| Processor implementation           | Platform-agnostic bridge to established processors |
| CSL &rarr; HTML converter          | CSL &rarr; your AST                                |

## Targets

| Target | Backend                                                                       | Formats                              | Status |
|--------|-------------------------------------------------------------------------------|--------------------------------------|--------|
| JVM    | [citeproc-java](https://github.com/michel-kraemer/citeproc-java) (Apache-2.0) | BibTeX, CSL JSON, YAML, EndNote, RIS | WIP    |
| WASM   | TBD                                                                           | TBD                                  | TBD    |

## Getting started

```kotlin
dependencies {
    implementation("com.quarkdown.bibliographer:bibliographer:0.3.0")
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
