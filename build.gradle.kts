plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
}

group = "com.quarkdown.bibliographer"
version = file("version.txt").readText().trim()

kotlin {
    explicitApi()
    jvmToolchain(11)

    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            api(libs.citeproc.java)
            runtimeOnly(libs.csl.locales)
        }
        jvmTest.dependencies {
            implementation(libs.csl.styles)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    version.set(libs.versions.ktlint.engine)
}

mavenPublishing {
    coordinates(group.toString(), "bibliographer", version.toString())

    publishToMavenCentral()

    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    pom {
        name.set("kotlin-bibliographer")
        description.set(
            "Kotlin Multiplatform bridge from CSL (Citation Style Language) processors " +
                "to a platform-agnostic bibliography token domain.",
        )
        url.set("https://github.com/quarkdown-labs/kotlin-bibliographer/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("iamgio")
                name.set("Giorgio Garofalo")
                url.set("https://github.com/iamgio")
            }
        }
        scm {
            url.set("https://github.com/quarkdown-labs/kotlin-bibliographer/")
            connection.set("scm:git:git://github.com/quarkdown-labs/kotlin-bibliographer.git")
            developerConnection.set("scm:git:ssh://git@github.com/quarkdown-labs/kotlin-bibliographer.git")
        }
    }
}
