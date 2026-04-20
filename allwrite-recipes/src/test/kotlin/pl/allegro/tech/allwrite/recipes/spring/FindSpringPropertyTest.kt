package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.properties
import pl.allegro.tech.allwrite.recipes.yaml

class FindSpringPropertyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FindSpringProperty("server.port", "8080", "*"))
            .validateRecipeSerialization(false)
    }

    @ParameterizedTest(name = "should find properties in {0} file")
    @ValueSource(strings = ["application.yml", "application.yaml", "application-dev.yml", "application-integration.yaml"])
    fun `should find spring properties in yaml application properties files`(fileName: String) {
        rewriteRun(
            yaml(
                before = """
                        server:
                           compression:
                               enabled: true
                           port: 8080
                        myapp:
                          i18n.enabled: true
                """.trimIndent(),
                after = """
                        server:
                           compression:
                               enabled: true
                           port: ~~>8080
                        myapp:
                          i18n.enabled: true
                """.trimIndent(),
                spec = { path("src/main/resources/$fileName") },
            ),
        )
    }

    @ParameterizedTest(name = "should find properties in {0} file")
    @ValueSource(strings = ["application.properties", "application-dev.properties"])
    fun `should find spring properties in properties application properties files`(fileName: String) {
        rewriteRun(
            properties(
                before = """
                        server.port: 8080
                """.trimIndent(),
                after = """
                        server.port: ~~>8080
                """.trimIndent(),
                spec = { path("src/main/resources/$fileName") },
            ),
        )
    }

    @Test
    fun `should not find spring properties in arbitrary files`() {
        rewriteRun(
            yaml(
                beforeAndAfter = """
                    server:
                      port: 8080
                """.trimIndent(),
                spec = { path("src/main/resources/backup.yml") },
            ),
            yaml(
                beforeAndAfter = """
                    server:
                      port: 8080
                """.trimIndent(),
                spec = { path("tycho.yaml") },
            ),
            properties(
                beforeAndAfter = """
                    server.port: 8080
                """.trimIndent(),
                spec = { path("src/main/resources/project.properties") },
            ),
        )
    }

    @Test
    fun `should find spring properties in profile-specific application properties files`() {
        rewriteRun(
            { spec ->
                spec.recipe(
                    FindSpringProperty(
                        propertyKey = "server.port",
                        expectedValue = "8080",
                        fileNameSuffix = "-integration",
                    ),
                )
            },
            yaml(
                before = """
                        server:
                           port: 8080
                """.trimIndent(),
                after = """
                        server:
                           port: ~~>8080
                """.trimIndent(),
                spec = { path("src/main/resources/application-integration.yml") },
            ),
            yaml(
                beforeAndAfter = """
                    server:
                      port: 8080
                """.trimIndent(),
                spec = { path("src/main/resources/application.yaml") },
            ),
            yaml(
                beforeAndAfter = """
                    server:
                      port: 8080
                """.trimIndent(),
                spec = { path("src/main/resources/application-dev.yaml") },
            ),
            properties(
                before = """
                        server.port: 8080
                """.trimIndent(),
                after = """
                        server.port: ~~>8080
                """.trimIndent(),
                spec = { path("src/main/resources/application-integration.properties") },
            ),
            properties(
                beforeAndAfter = """
                        server.port: 8080
                """.trimIndent(),
                spec = { path("src/main/resources/application.properties") },
            ),
            properties(
                beforeAndAfter = """
                        server.port: 8080
                """.trimIndent(),
                spec = { path("src/main/resources/application-dev.properties") },
            ),
        )
    }
}
