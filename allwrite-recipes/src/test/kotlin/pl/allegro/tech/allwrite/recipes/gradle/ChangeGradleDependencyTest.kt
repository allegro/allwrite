package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class ChangeGradleDependencyTest : RewriteTest {

    @Test
    fun `should change matching dependency in toml version catalog`() {
        rewriteRun(
            { spec ->
                spec.recipe(
                    ChangeGradleDependency(
                        oldGroupId = "org.junit.jupiter",
                        oldArtifactId = "junit-jupiter",
                        newArtifactId = "junit-jupiter-api",
                        newVersion = "5.11.0",
                    ),
                ).validateRecipeSerialization(false)
            },
            toml(
                before = """
                [libraries]
                junit = { group = "org.junit.jupiter", name = "junit-jupiter", version = "5.10.0" }
                """.trimIndent(),
                after = """
                [libraries]
                junit = { group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.11.0" }
                """.trimIndent(),
                { path("gradle/libs.versions.toml") },
            ),
        )
    }

    @Test
    fun `should change jackson blackbird dependency in gradle gradle kts and toml`() {
        rewriteRun(
            { spec ->
                spec.recipe(
                    ChangeGradleDependency(
                        oldGroupId = "com.fasterxml.jackson.module",
                        oldArtifactId = "jackson-module-afterburner",
                        newGroupId = "tools.jackson.module",
                        newArtifactId = "jackson-module-blackbird",
                        newVersion = "3.1.4",
                    ),
                ).validateRecipeSerialization(false)
            },
            buildGradle(
                before = """
                dependencies {
                    implementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation "tools.jackson.module:jackson-module-blackbird:3.1.4"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                before = """
                dependencies {
                    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2")
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation("tools.jackson.module:jackson-module-blackbird:3.1.4")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
            toml(
                before = """
                [libraries]
                jackson-module-blackbird = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "2.17.2" }
                """.trimIndent(),
                after = """
                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version = "3.1.4" }
                """.trimIndent(),
                { path("gradle/libs.versions.toml") },
            ),
            toml(
                before = """
                [versions]
                jackson-module-afterburner = "2.17.2"

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson-module-afterburner" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-blackbird = "3.1.4"

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                """.trimIndent(),
                { path("gradle/libs.versions.toml") },
            ),
        )
    }
}
