package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class DeleteSpringPropertyFromSpringAnnotationsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(DeleteSpringPropertyFromSpringAnnotations("myapp.test"))
            .withRecipeClasspath()
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should replace property in @SpringBootTest#properties`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(properties = {
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                })
                class Example {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(properties = {
                  "server.port=8080",
                  "myapp.best=2"
                })
                class Example {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(properties = [
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                ])
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(properties = [
                  "server.port=8080",
                  "myapp.best=2"
                ])
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(properties = [
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                ])
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(properties = [
                  "server.port=8080",
                  "myapp.best=2"
                ])
                class GroovyExample {}
                """.trimIndent()
            )
        )
    }

    @Test
    fun `should replace property in @SpringBootTest#value`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(value = {
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                })
                class Example {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(value = {
                  "server.port=8080",
                  "myapp.best=2"
                })
                class Example {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(value = [
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                ])
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(value = [
                  "server.port=8080",
                  "myapp.best=2"
                ])
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(value = [
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                ])
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(value = [
                  "server.port=8080",
                  "myapp.best=2"
                ])
                class GroovyExample {}
                """.trimIndent()
            )
        )
    }

    @Test
    fun `should replace property in @SpringBootTest#value and language-specific notation`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest("myapp.test=1")
                class Example {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest
                class Example {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest("server.port=8080", "myapp.test=1", "myapp.best=2")
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest("server.port=8080", "myapp.best=2")
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(value = "myapp.test=1")
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest
                class GroovyExample {}
                """.trimIndent()
            )
        )
    }

    @Test
    fun `should replace property in @TestPropertySource#properties`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.test.context.TestPropertySource;
                @TestPropertySource(properties = {
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                })
                class Example {}
                """.trimIndent(),
                after = """
                import org.springframework.test.context.TestPropertySource;
                @TestPropertySource(properties = {
                  "server.port=8080",
                  "myapp.best=2"
                })
                class Example {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.test.context.TestPropertySource
                @TestPropertySource(properties = [
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                ])
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.test.context.TestPropertySource
                @TestPropertySource(properties = [
                  "server.port=8080",
                  "myapp.best=2"
                ])
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.test.context.TestPropertySource
                @TestPropertySource(properties = [
                  "server.port=8080",
                  "myapp.test=1",
                  "myapp.best=2"
                ])
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.test.context.TestPropertySource
                @TestPropertySource(properties = [
                  "server.port=8080",
                  "myapp.best=2"
                ])
                class GroovyExample {}
                """.trimIndent()
            )
        )
    }

    @Test
    fun `should remove properties param from annotation if no other properties present`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.test.context.TestPropertySource;
                import org.springframework.boot.test.context.SpringBootTest;
                @TestPropertySource(properties = { "myapp.test=1" }, encoding = "UTF-8")
                @SpringBootTest(args = { "test" }, properties = { "myapp.test=1" })
                class Example {}
                """.trimIndent(),
                after = """
                import org.springframework.test.context.TestPropertySource;
                import org.springframework.boot.test.context.SpringBootTest;
                @TestPropertySource(encoding = "UTF-8")
                @SpringBootTest(args = { "test" })
                class Example {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.test.context.TestPropertySource
                import org.springframework.boot.test.context.SpringBootTest
                @TestPropertySource(properties = [ "myapp.test=1" ], encoding = "UTF-8")
                @SpringBootTest(args = [ "test" ], properties = [ "myapp.test=1" ])
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.test.context.TestPropertySource
                import org.springframework.boot.test.context.SpringBootTest
                @TestPropertySource(encoding = "UTF-8")
                @SpringBootTest(args = [ "test" ])
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.test.context.TestPropertySource
                import org.springframework.boot.test.context.SpringBootTest
                @TestPropertySource(properties = [ "myapp.test=1" ], encoding = "UTF-8")
                @SpringBootTest(args = [ "test" ], properties = [ "myapp.test=1" ])
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.test.context.TestPropertySource
                import org.springframework.boot.test.context.SpringBootTest
                @TestPropertySource(encoding = "UTF-8")
                @SpringBootTest(args = [ "test" ])
                class GroovyExample {}
                """.trimIndent()
            )
        )
    }

    @Test
    fun `should preserve last argument formatting`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(
                   // this comment should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT   , // this should stay
                   // this should disappear
                   properties = { "myapp.test=1" } // this comment should
                   // this too
                )
                class JavaExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(
                   // this comment should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT // this should stay
                )
                class JavaExample {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this comment should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT   , // this should stay
                   // this should disappear
                   properties = [ "myapp.test=1" ] // this comment should disappear
                   // this too
                )
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this comment should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT // this should stay
                )
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this comment should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT   , // this should stay
                   // this should disappear
                   properties = [ "myapp.test=1" ] // this comment should disappear
                   // this too
                )
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this comment should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT // this should stay
                )
                class GroovyExample {}
                """.trimIndent()
            )
        )

        rewriteRun(
            java(
                before = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(
                   // this should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT   ,
                   // this should disappear
                   properties = { "myapp.test=1" } // this comment should disappear
                   // this too
                )
                class JavaExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest;
                @SpringBootTest(
                   // this should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
                )
                class JavaExample {}
                """.trimIndent()
            ),
            kotlin(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT   ,
                   // this should disappear
                   properties = [ "myapp.test=1" ] // this comment should disappear
                   // this too
                )
                class KotlinExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
                )
                class KotlinExample {}
                """.trimIndent()
            ),
            groovy(
                before = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT   ,
                   // this should disappear
                   properties = [ "myapp.test=1" ] // this comment should disappear
                   // this too
                )
                class GroovyExample {}
                """.trimIndent(),
                after = """
                import org.springframework.boot.test.context.SpringBootTest
                @SpringBootTest(
                   // this should stay
                   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
                )
                class GroovyExample {}
                """.trimIndent()
            )
        )
    }
}
