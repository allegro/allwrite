package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class AddTomlVersionCatalogPluginTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(
                AddTomlVersionCatalogPlugin(
                    pluginName = "example",
                    pluginId = "com.example.plugin",
                    pluginVersion = "1.2.3",
                ),
            )
            .expectedCyclesThatMakeChanges(2)
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should add a plugin and preserve library version references`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    example-webmvc = { group = "com.example", name = "example-webmvc" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    example-webmvc = { group = "com.example", name = "example-webmvc" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    plugins {
                        id("java")
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        id("java")
                        alias(libs.plugins.example)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should add a plugin independently of matching library aliases`() {
        rewriteRun(
            toml(
                before = """
                    [libraries]
                    other = { group = "com.other", name = "other" }
                """.trimIndent(),
                after = """
                    [libraries]
                    other = { group = "com.other", name = "other" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    plugins {
                        id("java")
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        id("java")
                        alias(libs.plugins.example)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should apply a plugin alias when the plugins block is missing`() {
        rewriteRun(
            toml(
                before = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }
                """.trimIndent(),
                after = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    repositories {
                        mavenCentral()
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        alias(libs.plugins.example)
                    }

                    repositories {
                        mavenCentral()
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should apply a plugin alias to a Groovy build script`() {
        rewriteRun(
            toml(
                before = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }
                """.trimIndent(),
                after = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradle(
                before = """
                    plugins {
                        id 'java'
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        id 'java'
                        alias(libs.plugins.example)
                    }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should append a plugin to an existing plugins table`() {
        rewriteRun(
            toml(
                before = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }

                    [plugins]
                    kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                """.trimIndent(),
                after = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }

                    [plugins]
                    kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should update an existing plugin and preserve library version references`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "0.9.0"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }

                    [plugins]
                    example = { id = "com.example.plugin", version.ref = "example" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "0.9.0"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should not change a version catalog outside the expected path`() {
        rewriteRun(
            { spec -> spec.expectedCyclesThatMakeChanges(0) },
            toml(
                beforeAndAfter = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                """.trimIndent(),
            ) { path("other/libs.versions.toml") },
        )
    }

    @Test
    fun `should not duplicate a plugin when its name is already used`() {
        rewriteRun(
            { spec -> spec.expectedCyclesThatMakeChanges(0) },
            toml(
                beforeAndAfter = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }

                    [plugins]
                    example = { id = "com.other.plugin", version = "4.5.6" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        alias(libs.plugins.example)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }
}
