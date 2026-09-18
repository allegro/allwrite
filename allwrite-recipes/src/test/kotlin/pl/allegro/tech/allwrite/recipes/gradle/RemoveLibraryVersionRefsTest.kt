package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class RemoveLibraryVersionRefsTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(RemoveLibraryVersionRefs("example"))
            .expectedCyclesThatMakeChanges(1)
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should remove references from matching libraries and preserve unrelated entries`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example = { group = "com.example", name = "example", version.ref = "example" }
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                    unversioned = { group = "com.example", name = "unversioned" }

                    [plugins]
                    example = { id = "com.example.plugin", version.ref = "example" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example = { group = "com.example", name = "example" }
                    example-bom = { group = "com.example", name = "example-bom" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                    unversioned = { group = "com.example", name = "unversioned" }

                    [plugins]
                    example = { id = "com.example.plugin", version.ref = "example" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should not change a catalog outside the expected path`() {
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
    fun `should not change libraries without matching aliases`() {
        rewriteRun(
            { spec -> spec.expectedCyclesThatMakeChanges(0) },
            toml(
                beforeAndAfter = """
                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should remove references when a single consumer applies the expected plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveLibraryVersionRefs(
                            pluginName = "example",
                            applyToModulesWithPluginId = "application",
                        ),
                    )
                    .expectedCyclesThatMakeChanges(1)
                    .validateRecipeSerialization(false)
            },
            toml(
                before = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    example-starter-core = { group = "com.example", name = "example-starter-core", version.ref = "example" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }
                    example-starter-core = { group = "com.example", name = "example-starter-core" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        application
                    }

                    dependencies {
                        implementation(libs.example.bom)
                        implementation("com.example:example-starter-core:1.2.3")
                    }
                """.trimIndent(),
            ) { path("app/build.gradle.kts") },
        )
    }

    @Test
    fun `should not change the catalog when no module applies the expected plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveLibraryVersionRefs(
                            pluginName = "example",
                            applyToModulesWithPluginId = "application",
                        ),
                    )
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            toml(
                beforeAndAfter = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        id("java-library")
                    }

                    dependencies {
                        implementation(libs.example.bom)
                    }
                """.trimIndent(),
            ) { path("lib/build.gradle.kts") },
        )
    }

    @Test
    fun `should remove references when all referencing modules apply the expected plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveLibraryVersionRefs(
                            pluginName = "example",
                            applyToModulesWithPluginId = "application",
                        ),
                    )
                    .expectedCyclesThatMakeChanges(1)
                    .validateRecipeSerialization(false)
            },
            toml(
                before = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        application
                    }

                    dependencies {
                        implementation(libs.example.bom)
                    }
                """.trimIndent(),
            ) { path("app/build.gradle.kts") },
            buildGradle(
                beforeAndAfter = """
                    plugins {
                        id 'application'
                    }

                    dependencies {
                        implementation libs.example.bom
                    }
                """.trimIndent(),
            ) { path("worker/build.gradle") },
        )
    }

    @Test
    fun `should leave the catalog unchanged when any referencing module does not apply the expected plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveLibraryVersionRefs(
                            pluginName = "example",
                            applyToModulesWithPluginId = "application",
                        ),
                    )
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            toml(
                beforeAndAfter = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        application
                    }

                    dependencies {
                        implementation(libs.example.bom)
                    }
                """.trimIndent(),
            ) { path("app/build.gradle.kts") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        id("java-library")
                    }

                    dependencies {
                        add("implementation", libs.example.bom)
                        add("implementation", "com.example:example-bom")
                    }

                    dependencies.add("implementation", libs.example.bom)
                """.trimIndent(),
            ) { path("lib/build.gradle.kts") },
        )
    }

    @Test
    fun `should leave bundle references unchanged`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveLibraryVersionRefs(
                            pluginName = "example",
                            applyToModulesWithPluginId = "application",
                        ),
                    )
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            toml(
                beforeAndAfter = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other" }

                    [bundles]
                    example = ["example-bom", "other"]
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        application
                    }

                    dependencies {
                        implementation(libs.bundles.example)
                    }
                """.trimIndent(),
            ) { path("app/build.gradle.kts") },
        )
    }
}
