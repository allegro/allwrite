package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class RemoveTomlVersionCatalogLibraryTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(RemoveTomlVersionCatalogLibrary("com.example", "example-bom"))
            .expectedCyclesThatMakeChanges(1)
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should remove a platform dependency and its catalog entry`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    other = "4.5.6"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(platform(libs.example.bom))
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should remove a Groovy platform dependency and its catalog entry`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    other = "4.5.6"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradle(
                before = """
                    dependencies {
                        implementation platform(libs.example.bom)
                        implementation libs.other
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation libs.other
                    }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should remove an enforced platform dependency and its catalog entry`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    other = "4.5.6"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        api(enforcedPlatform(libs.example.bom))
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should remove a plain catalog dependency and its catalog entry`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    other = "4.5.6"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(libs.example.bom)
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should remove literal GAV dependencies without a catalog entry`() {
        rewriteRun(
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(platform("com.example:example-bom:1.2.3"))
                        api("com.example:example-bom")
                        implementation("com.other:other:4.5.6")
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation("com.other:other:4.5.6")
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should remove a library declared with module notation`() {
        rewriteRun(
            toml(
                before = """
                    [libraries]
                    example-bom = { module = "com.example:example-bom" }
                    other = { group = "com.other", name = "other" }
                """.trimIndent(),
                after = """
                    [libraries]
                    other = { group = "com.other", name = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(libs.example.bom)
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should remove gated usages and catalog entry only for modules with expected plugin id`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveTomlVersionCatalogLibrary(
                            groupId = "com.example",
                            artifactId = "example-bom",
                            applyToModulesWithPluginId = "application",
                        ),
                    )
                    .validateRecipeSerialization(false)
            },
            toml(
                beforeAndAfter = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    plugins {
                        application
                    }

                    dependencies {
                        implementation(platform(libs.example.bom))
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        application
                    }

                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("app/build.gradle.kts") },
            buildGradle(
                before = """
                    plugins {
                        id 'application'
                    }

                    dependencies {
                        implementation platform(libs.example.bom)
                        implementation libs.other
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        id 'application'
                    }

                    dependencies {
                        implementation libs.other
                    }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        id("java-library")
                    }

                    dependencies {
                        implementation(platform(libs.example.bom))
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("lib/build.gradle.kts") },
        )
    }

    @Test
    fun `should not change anything when no module passes the gate`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(
                        RemoveTomlVersionCatalogLibrary(
                            groupId = "com.example",
                            artifactId = "example-bom",
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
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        id("java-library")
                    }

                    dependencies {
                        implementation(platform(libs.example.bom))
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("lib/build.gradle.kts") },
        )
    }

    @Test
    fun `should leave a bundled alias and its usages unchanged`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(RemoveTomlVersionCatalogLibrary("com.example", "example"))
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            toml(
                beforeAndAfter = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example = { group = "com.example", name = "example", version.ref = "example" }
                    other = { group = "com.other", name = "other" }

                    [bundles]
                    example = ["example", "other"]
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                beforeAndAfter = """
                    dependencies {
                        implementation(libs.bundles.example)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should keep a version referenced by a plugin`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }

                    [plugins]
                    example = { id = "com.example.plugin", version.ref = "example" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }

                    [plugins]
                    example = { id = "com.example.plugin", version.ref = "example" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    plugins {
                        alias(libs.plugins.example)
                    }

                    dependencies {
                        implementation(platform(libs.example.bom))
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    plugins {
                        alias(libs.plugins.example)
                    }

                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should keep a version referenced by another library`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    example-other = { group = "com.example", name = "example-other", version.ref = "example" }
                """.trimIndent(),
                after = """
                    [versions]
                    example = "1.2.3"

                    [libraries]
                    example-other = { group = "com.example", name = "example-other", version.ref = "example" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(libs.example.bom)
                        implementation(libs.example.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.example.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should not remove unrelated orphaned versions`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example = "1.2.3"
                    orphan = "4.5.6"
                    other = "7.8.9"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    orphan = "4.5.6"
                    other = "7.8.9"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(libs.example.bom)
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should keep a version referenced through a normalized accessor`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    example-version = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version.ref = "example-version" }
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
                after = """
                    [versions]
                    example-version = "1.2.3"
                    other = "4.5.6"

                    [libraries]
                    other = { group = "com.other", name = "other", version.ref = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    val exampleVersion = libs.versions.example.version.get()

                    dependencies {
                        implementation(libs.example.bom)
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    val exampleVersion = libs.versions.example.version.get()

                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should remove a library with an inline version`() {
        rewriteRun(
            toml(
                before = """
                    [libraries]
                    example-bom = { group = "com.example", name = "example-bom", version = "1.2.3" }
                    other = { group = "com.other", name = "other" }
                """.trimIndent(),
                after = """
                    [libraries]
                    other = { group = "com.other", name = "other" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradleKts(
                before = """
                    dependencies {
                        implementation(libs.example.bom)
                        implementation(libs.other)
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        implementation(libs.other)
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }
}
