package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class ChangeGradleDependencyTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe()).validateRecipeSerialization(false)
    }

    @Test
    fun `should change dependency in build gradle`() {
        rewriteRun(
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
        )
    }

    @Test
    fun `should change dependency in build gradle kts`() {
        rewriteRun(
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
        )
    }

    @Test
    fun `should change dependency in toml version value`() {
        rewriteRun(
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
        )
    }

    @Test
    fun `should change dependency in toml version ref`() {
        rewriteRun(
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

    @Test
    fun `should not change non matching dependency in build gradle`() {
        rewriteRun(
            buildGradle(
                """
                dependencies {
                    implementation "org.junit.jupiter:junit-jupiter:5.10.0"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should not change non matching dependency in toml`() {
        rewriteRun(
            toml(
                """
                [libraries]
                junit = { group = "org.junit.jupiter", name = "junit-jupiter", version = "5.10.0" }
                """.trimIndent(),
                { path("gradle/libs.versions.toml") },
            ),
        )
    }

    @Test
    fun `should rename only targeted version refs in toml`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson-module-afterburner = "2.17.2"
                jackson-bom = "2.17.2"

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson-module-afterburner" }
                jackson-bom = { group = "com.fasterxml.jackson", name = "jackson-bom", version.ref = "jackson-bom" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-blackbird = "3.1.4"
                jackson-bom = "2.17.2"

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                jackson-bom = { group = "com.fasterxml.jackson", name = "jackson-bom", version.ref = "jackson-bom" }
                """.trimIndent(),
                { path("gradle/libs.versions.toml") },
            ),
        )
    }

    @Test
    fun `should keep version unchanged when new version is null`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = null)).validateRecipeSerialization(false)
            },
            toml(
                before = """
                [libraries]
                jackson-module-blackbird = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "2.17.2" }
                """.trimIndent(),
                after = """
                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version = "2.17.2" }
                """.trimIndent(),
                { path("gradle/libs.versions.toml") },
            ),
        )
    }

    private fun recipe(newVersion: String? = "3.1.4"): ChangeGradleDependency =
        ChangeGradleDependency(
            oldGroupId = "com.fasterxml.jackson.module",
            oldArtifactId = "jackson-module-afterburner",
            newGroupId = "tools.jackson.module",
            newArtifactId = "jackson-module-blackbird",
            newVersion = newVersion,
        )
}
