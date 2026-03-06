package pl.allegro.tech.allwrite.recipes.properties

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.properties

class FindPropertiesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FindProperties("server-compression.enabled", "true"))
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should find properties`() {
        rewriteRun(
            properties(
                before = """
                    server.enabled: true
                    server.port: 8080
                    server-compression.enabled: true
                    """.trimIndent(),
                after = """
                    server.enabled: true
                    server.port: 8080
                    server-compression.enabled: ~~>true
                    """.trimIndent(),
                spec = { path("application.properties") },
            )
        )
    }

    @Test
    fun `should find properties using relaxed binding`() {
        rewriteRun(
            properties(
                before = """
                    server.enabled: true
                    server.port: 8080
                    serverCompression.enabled: true
                    """.trimIndent(),
                after = """
                    server.enabled: true
                    server.port: 8080
                    serverCompression.enabled: ~~>true
                    """.trimIndent(),
                spec = { path("application.properties") },
            )
        )
    }

    @Test
    fun `should find with glob pattern`() {
        rewriteRun(
            {
                spec -> spec.recipe(FindProperties("server*", "true"))
            },
            properties(
                before = """
                    server.enabled: true
                    server.port: 8080
                    server.compression.enabled: true
                    """.trimIndent(),
                after = """
                    server.enabled: ~~>true
                    server.port: 8080
                    server.compression.enabled: ~~>true
                    """.trimIndent(),
                spec = { path("application.properties") },
            )
        )
    }
}
