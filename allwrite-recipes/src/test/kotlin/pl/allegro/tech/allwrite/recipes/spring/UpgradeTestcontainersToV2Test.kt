package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class UpgradeTestcontainersToV2Test : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(
            object : Recipe() {
                override fun getDisplayName(): String = "Upgrade Testcontainers to v2"

                override fun getDescription(): String = "Renames Testcontainers v2 dependencies."

                override fun getRecipeList(): List<Recipe> = upgradeTestcontainersToV2()
            },
        )
    }

    @Test
    fun `should rename testcontainers libraries`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    testcontainers = "2.0.0"

                    [libraries]
                    mongodb = { module = "org.testcontainers:mongodb", version.ref = "testcontainers" }
                    junit = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
                """.trimIndent(),
                after = """
                    [versions]

                    [libraries]
                    testcontainers-mongodb = { group = "org.testcontainers", name = "testcontainers-mongodb" }
                    junit = { group = "org.testcontainers", name = "testcontainers-junit-jupiter" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradle(
                before = "dependencies { implementation(\"org.testcontainers:mongodb:1.20.0\") }",
                after = "dependencies { implementation(\"org.testcontainers:testcontainers-mongodb\") }",
            ),
            buildGradleKts(
                before = "dependencies { implementation(\"org.testcontainers:junit-jupiter:1.20.0\") }",
                after = "dependencies { implementation(\"org.testcontainers:testcontainers-junit-jupiter\") }",
            ),
        )
    }
}
