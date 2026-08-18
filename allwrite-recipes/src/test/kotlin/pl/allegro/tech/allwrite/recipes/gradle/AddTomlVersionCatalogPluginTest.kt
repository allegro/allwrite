package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.toml

class AddTomlVersionCatalogPluginTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(
                AddTomlVersionCatalogPlugin(
                    pluginName = "example",
                    pluginId = "com.example.plugin",
                    pluginVersion = "1.2.3",
                    requiredLibraryGroup = "com.example",
                    requiredLibraryName = "example-*",
                ),
            )
            .expectedCyclesThatMakeChanges(2)
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should add a plugin and remove the required library version reference`() {
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
                    example-bom = { group = "com.example", name = "example-bom" }
                    example-webmvc = { group = "com.example", name = "example-webmvc" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should not add a plugin when the required library is missing`() {
        rewriteRun(
            { spec -> spec.expectedCyclesThatMakeChanges(0) },
            toml(
                beforeAndAfter = """
                    [libraries]
                    other = { group = "com.other", name = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
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
    fun `should update an existing plugin and remove the required library version reference`() {
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
                    example-bom = { group = "com.example", name = "example-bom" }

                    [plugins]
                    example = { id = "com.example.plugin", version = "1.2.3" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should remove references from all matching libraries and preserve unrelated references`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    example-core = { group = "com.example", name = "example-core", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }
                    example-core = { group = "com.example", name = "example-core" }
                    other = { group = "com.other", name = "other", version.ref = "other" }

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
            { spec -> spec.expectedCyclesThatMakeChanges(1) },
            toml(
                before = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }

                    [plugins]
                    example = { id = "com.other.plugin", version = "4.5.6" }
                """.trimIndent(),
                after = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }

                    [plugins]
                    example = { id = "com.other.plugin", version = "4.5.6" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }
}
