package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class RelocateAutoConfigureWebTestClientTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(RelocateAutoConfigureWebTestClient())
            .withRecipeClasspath()
    }

    @Test
    fun `should relocate test client imports`() {
        // given
        rewriteRun(
            kotlin(
                before = """
                    import org.springframework.boot.test.web.client.TestRestTemplate
                    import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer

                    class Example(
                        val restTemplate: TestRestTemplate,
                        val webTestClientCustomizer: WebTestClientBuilderCustomizer,
                    ) {
                        val oldType = "org.springframework.boot.test.web.client.TestRestTemplate"
                    }
                """.trimIndent(),
                after = """
                    import org.springframework.boot.resttestclient.TestRestTemplate
                    import org.springframework.boot.webtestclient.autoconfigure.WebTestClientBuilderCustomizer

                    class Example(
                        val restTemplate: TestRestTemplate,
                        val webTestClientCustomizer: WebTestClientBuilderCustomizer,
                    ) {
                        val oldType = "org.springframework.boot.test.web.client.TestRestTemplate"
                    }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should not change new annotation import`() {
        // given
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                    import org.springframework.boot.webtestclient.autoconfigure.WebTestClientBuilderCustomizer

                    class Example
                """.trimIndent(),
            ),
        )
    }
}
