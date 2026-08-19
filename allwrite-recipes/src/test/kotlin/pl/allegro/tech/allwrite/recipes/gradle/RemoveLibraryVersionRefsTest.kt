package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
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
}
