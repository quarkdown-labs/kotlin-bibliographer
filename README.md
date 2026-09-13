# kotlin-bibliographer demo

A small static demo of [kotlin-bibliographer](https://github.com/quarkdown-labs/kotlin-bibliographer)'s
`wasmJs` target, running the Kotlin Multiplatform API in the browser:
[`StyleCatalog`](https://github.com/quarkdown-labs/kotlin-bibliographer/blob/main/src/commonMain/kotlin/com/quarkdown/bibliographer/StyleCatalog.kt)
populates the style picker, `Bibliographer` renders citations and bibliographies,
and the `BibliographyToken` tree is converted to DOM nodes.

This branch is a standalone Kotlin/Wasm project, independent from `main`.
GitHub Pages serves the prebuilt distribution committed in [`docs/`](docs/).

## Rebuilding the site

```sh
./gradlew updateSite
```

builds the production distribution and syncs it into `docs/`.
