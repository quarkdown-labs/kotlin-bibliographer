import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.4.20"
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation("com.quarkdown.bibliographer:bibliographer:0.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
        }
    }
}

// Rebuilds the GitHub Pages site (docs/) from the production distribution.
tasks.register<Sync>("updateSite") {
    from(tasks.named("wasmJsBrowserDistribution"))
    into(layout.projectDirectory.dir("docs"))
    preserve { include(".nojekyll") }
}
