package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.toml

internal class PreconditionsAwareAddDependencyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe())
    }

    @Nested
    inner class GradleBuildCases {

        @Test
        fun `should add dependency when any configured type is used`() {
            // given
            rewriteRun(
                java(
                    beforeAndAfter = """
                        import java.util.Set;

                        class Example {
                            Set<String> values;
                        }
                    """.trimIndent(),
                ),
                buildGradle(
                    before = """
                        dependencies {
                        }
                    """.trimIndent(),
                    after = """
                        dependencies {
                            testImplementation("com.example:example-dependency")
                        }
                    """.trimIndent(),
                ),
            )
        }

        @Test
        fun `should not add dependency when no configured type is used`() {
            // given
            rewriteRun(
                { spec -> spec.expectedCyclesThatMakeChanges(0) },
                java(beforeAndAfter = "class Example {}"),
                buildGradle(
                    beforeAndAfter = """
                        dependencies {
                        }
                    """.trimIndent(),
                ),
            )
        }
    }

    @Nested
    inner class TomlVersionCatalogCases {

        @Test
        fun `should add version catalog dependency when a configured type is used`() {
            // given
            rewriteRun(
                { spec -> spec.recipe(recipe(versionCatalogName = "example")) },
                java(
                    beforeAndAfter = """
                        import java.util.List;

                        class Example {
                            List<String> values;
                        }
                    """.trimIndent(),
                ),
                toml(
                    before = """
                        [libraries]
                        existing = { module = "com.example:existing" }
                    """.trimIndent(),
                    after = """
                        [libraries]
                        existing = { module = "com.example:existing" }
                        example = { group = "com.example", name = "example-dependency" }
                    """.trimIndent(),
                ) { path("gradle/libs.versions.toml") },
                buildGradle(
                    before = """
                        dependencies {
                        }
                    """.trimIndent(),
                    after = """
                        dependencies {
                            testImplementation(libs.example)
                        }
                    """.trimIndent(),
                ),
            )
        }

        @Test
        fun `should not duplicate an existing version catalog dependency`() {
            // given
            rewriteRun(
                { spec ->
                    spec.recipe(recipe(versionCatalogName = "example"))
                        .expectedCyclesThatMakeChanges(0)
                },
                java(
                    beforeAndAfter = """
                        import java.util.List;

                        class Example {
                            List<String> values;
                        }
                    """.trimIndent(),
                ),
                toml(
                    beforeAndAfter = """
                        [libraries]
                        example = { module = "com.example:example-dependency" }
                    """.trimIndent(),
                ) { path("gradle/libs.versions.toml") },
            )
        }
    }

    private fun recipe(
        versionCatalogName: String = "example-dependency",
        requiredClasspath: List<String> = emptyList(),
    ): PreconditionsAwareAddDependency =
        PreconditionsAwareAddDependency(
            requiredClasspath = requiredClasspath,
            detectedTypes = listOf("java.util.List", "java.util.Set"),
            configuration = "testImplementation",
            groupId = "com.example",
            artifactId = "example-dependency",
            versionCatalogName = versionCatalogName,
        )
}
