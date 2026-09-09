# kotlin-bibliographer

A Kotlin Multiplatform bridge from [Citation Style Language](https://citationstyles.org) (`.csl`) processors
to a platform-agnostic bibliography **token domain**.

[Quarkdown](https://github.com/iamgio/quarkdown) is the reference consumer.

| :x: What this library is ***not*** | :white_check_mark: What it ***is***                |
|------------------------------------|----------------------------------------------------|
| Processor implementation           | Platform-agnostic bridge to established processors |
| CSL &rarr; HTML converter          | CSL &rarr; your AST                                |

## Targets

| Target | Backend                                                                       | Status |
|--------|-------------------------------------------------------------------------------|--------|
| JVM    | [citeproc-java](https://github.com/michel-kraemer/citeproc-java) (Apache-2.0) | WIP    |
| WASM   | TBD                                                                           | TBD    |
