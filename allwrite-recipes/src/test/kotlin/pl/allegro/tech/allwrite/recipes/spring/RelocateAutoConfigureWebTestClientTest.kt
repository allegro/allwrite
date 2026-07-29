package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.kotlin

class RelocateAutoConfigureWebTestClientTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(RelocateAutoConfigureWebTestClient())
            .parser(KotlinParser.builder().classpathFromResources(InMemoryExecutionContext(), "spring-boot-test-3"))
    }

    @Test
    fun `should relocate annotation import`() {
        rewriteRun(
            kotlin(
                before = """
                    import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient

                    class Example
                """.trimIndent(),
                after = """
                    import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient

                    class Example
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should not change new annotation import`() {
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                    import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient

                    class Example
                """.trimIndent(),
            ),
        )
    }
}
