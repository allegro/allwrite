package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.toml

class AddRestAssuredSpringWebTestClientEntryTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(AddRestAssuredSpringWebTestClientEntry())
    }

    @Test
    fun `should add entry when restAssured exists`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    restAssured = "5.5.0"

                    [libraries]
                    rest-assured = { module = "io.rest-assured:rest-assured", version.ref = "restAssured" }
                """.trimIndent(),
                after = """
                    [versions]
                    restAssured = "5.5.0"

                    [libraries]
                    rest-assured = { module = "io.rest-assured:rest-assured", version.ref = "restAssured" }
                    spring-web-test-client = { group = "io.rest-assured", name = "spring-web-test-client" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradle(
                before = "dependencies {\n    testImplementation(libs.rest.assured)\n}\n",
                after = """
                    dependencies {
                        testImplementation(libs.rest.assured)
                        testImplementation(libs.spring.web.test.client)
                    }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should add dependency without version catalog`() {
        rewriteRun(
            buildGradle(
                before = """
                    dependencies {
                        testImplementation("io.rest-assured:rest-assured:5.5.0")
                    }
                """.trimIndent(),
                after = """
                    dependencies {
                        testImplementation("io.rest-assured:rest-assured:5.5.0")
                        testImplementation("io.rest-assured:spring-web-test-client")
                    }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should not duplicate entry`() {
        rewriteRun(
            toml(
                beforeAndAfter = """
                    [versions]
                    restAssured = "5.5.0"

                    [libraries]
                    spring-web-test-client = { group = "io.rest-assured", name = "spring-web-test-client", version.ref = "restAssured" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should not add dependency when restAssured does not exist`() {
        // given
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                    dependencies {
                        testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
                    }
                """.trimIndent(),
            ),
        )
    }
}
