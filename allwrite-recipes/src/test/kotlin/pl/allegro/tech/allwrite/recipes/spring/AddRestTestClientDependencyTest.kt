package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class AddRestTestClientDependencyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(AddRestTestClientDependency())
            .withRecipeClasspath()
    }

    @Test
    fun `should add REST test client dependency for TestRestTemplate`() {
        // given
        rewriteRun(
            java(
                beforeAndAfter = """
                    import org.springframework.boot.test.web.client.TestRestTemplate;

                    class Example {
                        TestRestTemplate restTemplate;
                    }
                """.trimIndent(),
            ) { path("src/test/java/Example.java") },
            buildGradle(
                before = "dependencies {\n}\n",
                after = """dependencies {
                    |    testImplementation("org.springframework.boot:spring-boot-resttestclient")
                    |}
                """.trimMargin(),
            ),
        )
    }

    @Test
    fun `should not add REST test client dependency without usage`() {
        // given
        rewriteRun(
            java(beforeAndAfter = "class Example {}") { path("src/main/java/Example.java") },
        )
    }
}
